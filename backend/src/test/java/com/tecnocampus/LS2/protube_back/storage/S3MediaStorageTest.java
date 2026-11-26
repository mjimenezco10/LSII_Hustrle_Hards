package com.tecnocampus.LS2.protube_back.storage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * The S3 storage against a real S3-compatible server (SeaweedFS in Docker), each test with its own bucket.
 */
class S3MediaStorageTest {

    private static final byte[] VIDEO = "video bytes".getBytes(StandardCharsets.UTF_8);

    static S3Client s3;

    @TempDir
    Path store;

    String bucket;
    S3MediaStorage storage;

    @BeforeAll
    static void startServer() {
        s3 = S3TestServer.client();
    }

    @AfterAll
    static void closeClient() {
        s3.close();
    }

    @BeforeEach
    void setUp() {
        bucket = "test-" + UUID.randomUUID();
        storage = new S3MediaStorage(s3, bucket, S3TestServer.endpoint() + "/" + bucket + "/");
        storage.createBucketIfMissing();
    }

    private byte[] read(String key) {
        ResponseBytes<GetObjectResponse> object = s3.getObjectAsBytes(request -> request.bucket(bucket).key(key));
        return object.asByteArray();
    }

    private String contentTypeOf(String key) {
        return s3.headObject(request -> request.bucket(bucket).key(key)).contentType();
    }

    @Test
    void createsAPublicBucketWhoseFilesAnyoneCanDownload() throws Exception {
        storage.save("upload-1.mp4", new MockMultipartFile("video", "a.mp4", "video/mp4", VIDEO));

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(storage.publicUrl("upload-1.mp4")).build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        assertArrayEquals(VIDEO, response.body());
    }

    @Test
    void leavesAnExistingBucketAsItIs() throws IOException {
        storage.save("upload-1.mp4", new MockMultipartFile("video", "a.mp4", "video/mp4", VIDEO));

        assertDoesNotThrow(storage::createBucketIfMissing);

        assertArrayEquals(VIDEO, read("upload-1.mp4"));
    }

    @Test
    void savesAnUploadedFileWithItsContentType() throws IOException {
        storage.save("avatars/a.webp", new MockMultipartFile("avatar", "a.webp", "image/webp", VIDEO));

        assertArrayEquals(VIDEO, read("avatars/a.webp"));
        assertEquals("image/webp", contentTypeOf("avatars/a.webp"));
    }

    @Test
    void importsAFileOfTheStoreWithTheContentTypeOfItsExtension() throws IOException {
        Path file = Files.write(store.resolve("abc.mp4"), VIDEO);

        storage.importFile("abc.mp4", file);

        assertArrayEquals(VIDEO, read("abc.mp4"));
        assertEquals("video/mp4", contentTypeOf("abc.mp4"));
    }

    @Test
    void doesNotUploadAgainAFileAlreadyInTheBucket() throws IOException {
        s3.putObject(request -> request.bucket(bucket).key("abc.webp"), RequestBody.fromString("already there"));
        Path file = Files.write(store.resolve("abc.webp"), VIDEO);

        storage.importFile("abc.webp", file);

        assertEquals("already there", new String(read("abc.webp"), StandardCharsets.UTF_8));
    }

    @Test
    void deletesAFile() throws IOException {
        storage.save("upload-1.mp4", new MockMultipartFile("video", "a.mp4", "video/mp4", VIDEO));

        storage.deleteQuietly("upload-1.mp4");

        assertThrows(NoSuchKeyException.class, () -> read("upload-1.mp4"));
    }

    @Test
    void deletingAMissingFileDoesNothing() {
        assertDoesNotThrow(() -> storage.deleteQuietly("missing.mp4"));
    }

    @Test
    void failsWithIOExceptionWhenTheBucketDoesNotExist() throws IOException {
        S3MediaStorage withoutBucket = new S3MediaStorage(s3, "missing-" + UUID.randomUUID(), "/storage");
        MockMultipartFile video = new MockMultipartFile("video", "a.mp4", "video/mp4", VIDEO);
        Path file = Files.write(store.resolve("abc.mp4"), VIDEO);

        assertThrows(IOException.class, () -> withoutBucket.save("upload-1.mp4", video));
        assertThrows(IOException.class, () -> withoutBucket.importFile("abc.mp4", file));
        assertDoesNotThrow(() -> withoutBucket.deleteQuietly("upload-1.mp4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsTheBucketEvenIfItCannotBeMadePublic() {
        S3Client client = Mockito.mock(S3Client.class);
        doThrow(NoSuchBucketException.builder().build()).when(client).headBucket(any(Consumer.class));
        doThrow(S3Exception.builder().message("not supported").build())
                .when(client).putBucketPolicy(any(Consumer.class));

        assertDoesNotThrow(new S3MediaStorage(client, "bucket", "/storage")::createBucketIfMissing);

        verify(client).createBucket(any(Consumer.class));
        verify(client).putBucketPolicy(any(Consumer.class));
    }

    @Test
    void publicUrlJoinsTheBaseUrlAndTheKey() {
        assertEquals(URI.create("/storage/avatars/a.webp"),
                new S3MediaStorage(s3, "bucket", "/storage/").publicUrl("avatars/a.webp"));
        assertEquals(URI.create("https://cdn.example.com/x.mp4"),
                new S3MediaStorage(s3, "bucket", "https://cdn.example.com").publicUrl("x.mp4"));
    }

    @Test
    void unknownExtensionsAreUploadedAsBinary() {
        assertEquals("image/jpeg", S3MediaStorage.contentTypeOf("a.JPG"));
        assertEquals("application/octet-stream", S3MediaStorage.contentTypeOf("notes.txt"));
    }
}
