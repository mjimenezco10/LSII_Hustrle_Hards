package com.tecnocampus.LS2.protube_back.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.nio.file.Path;

/**
 * Chooses where the media files are kept with {@code pro_tube.storage.type} ({@code ENV_PROTUBE_STORAGE}):
 * {@code local} (default, the folder {@code pro_tube.store.dir}) or {@code s3} (a bucket, see application.properties).
 */
@Configuration
public class StorageConfig {

    public static final String TYPE_PROPERTY = "pro_tube.storage.type";

    @Bean
    @ConditionalOnProperty(name = TYPE_PROPERTY, havingValue = "local", matchIfMissing = true)
    public MediaStorage localMediaStorage(@Value("${pro_tube.store.dir}") String storeDir) {
        return new LocalMediaStorage(Path.of(storeDir));
    }

    @Bean
    @ConditionalOnProperty(name = TYPE_PROPERTY, havingValue = "s3")
    public S3MediaStorage s3MediaStorage(S3Client s3Client,
                                         @Value("${pro_tube.storage.s3.bucket}") String bucket,
                                         @Value("${pro_tube.storage.s3.public-url}") String publicUrl) {
        S3MediaStorage storage = new S3MediaStorage(s3Client, bucket, publicUrl);
        storage.createBucketIfMissing();
        return storage;
    }

    /**
     * With an endpoint (an S3-compatible server such as SeaweedFS) the bucket goes in the path
     * ({@code http://s3:8333/<bucket>/<key>}); without one the client talks to AWS. Without access keys it uses the
     * default AWS chain (env vars, profile, IAM role).
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = TYPE_PROPERTY, havingValue = "s3")
    public S3Client s3Client(@Value("${pro_tube.storage.s3.endpoint:}") String endpoint,
                             @Value("${pro_tube.storage.s3.region}") String region,
                             @Value("${pro_tube.storage.s3.access-key:}") String accessKey,
                             @Value("${pro_tube.storage.s3.secret-key:}") String secretKey) {
        AwsCredentialsProvider credentials = accessKey.isBlank()
                ? DefaultCredentialsProvider.create()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials);
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        return builder.build();
    }
}
