package com.tecnocampus.LS2.protube_back.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Keeps the media files in an S3 bucket (AWS S3, or SeaweedFS in {@code compose.yaml}) instead of the disk of the server.
 * The backend only writes and deletes the files: the browser downloads them straight from the bucket, because
 * {@code MediaRedirectController} redirects {@code /media/<key>} to {@link #publicUrl(String)}.
 */
public class S3MediaStorage implements MediaStorage {

    private static final Logger LOG = LoggerFactory.getLogger(S3MediaStorage.class);

    // Content type of the files of the video store, so the browser plays and shows them from the bucket
    static final Map<String, String> CONTENT_TYPES = Map.of(
            "mp4", "video/mp4", "webm", "video/webm",
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp");

    // Anyone can download the files, as with /media/** before: nobody can list, write or delete them
    private static final String PUBLIC_READ_POLICY = """
            {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},\
            "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}""";

    private final S3Client s3;
    private final String bucket;
    private final String publicBaseUrl;

    /**
     * @param publicBaseUrl URL the browser downloads the files of the bucket from, e.g.
     *                      {@code http://localhost:8080/storage} (nginx in front of SeaweedFS) or
     *                      {@code https://<bucket>.s3.<region>.amazonaws.com}
     */
    public S3MediaStorage(S3Client s3, String bucket, String publicBaseUrl) {
        this.s3 = s3;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    /**
     * Creates the bucket with public read access if it does not exist yet (a new SeaweedFS volume). An existing bucket
     * is left as it is, so on AWS its permissions are managed there.
     */
    public void createBucketIfMissing() {
        try {
            s3.headBucket(request -> request.bucket(bucket));
        } catch (NoSuchBucketException e) {
            s3.createBucket(request -> request.bucket(bucket));
            LOG.info("Created the bucket {}", bucket);
            try {
                s3.putBucketPolicy(request -> request.bucket(bucket).policy(PUBLIC_READ_POLICY.formatted(bucket)));
            } catch (S3Exception policyError) {
                // Some S3-compatible servers have no bucket policies: their files may still be public
                LOG.warn("Could not make the bucket {} public: {}", bucket, policyError.getMessage());
            }
        }
    }

    @Override
    public void save(String key, MultipartFile file) throws IOException {
        try (InputStream content = file.getInputStream()) {
            s3.putObject(putRequest(key, file.getContentType()), RequestBody.fromInputStream(content, file.getSize()));
        } catch (SdkException e) {
            throw new IOException("Could not upload " + key + " to the bucket " + bucket, e);
        }
    }

    @Override
    public void importFile(String key, Path file) throws IOException {
        try {
            if (exists(key)) {
                return;
            }
            s3.putObject(putRequest(key, contentTypeOf(key)), RequestBody.fromFile(file));
        } catch (SdkException e) {
            throw new IOException("Could not upload " + file + " to the bucket " + bucket, e);
        }
    }

    @Override
    public void deleteQuietly(String key) {
        try {
            s3.deleteObject(request -> request.bucket(bucket).key(key));
        } catch (SdkException e) {
            LOG.warn("Could not delete {} from the bucket {}", key, bucket, e);
        }
    }

    /**
     * URL the browser downloads the file with {@code key} from.
     */
    public URI publicUrl(String key) {
        return URI.create(publicBaseUrl + "/" + key);
    }

    private boolean exists(String key) {
        try {
            s3.headObject(request -> request.bucket(bucket).key(key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private PutObjectRequest putRequest(String key, String contentType) {
        return PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();
    }

    static String contentTypeOf(String key) {
        String extension = key.substring(key.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }
}
