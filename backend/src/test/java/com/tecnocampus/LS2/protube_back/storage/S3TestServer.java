package com.tecnocampus.LS2.protube_back.storage;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Throwaway S3-compatible server (SeaweedFS, the same image as compose.yaml) shared by the tests of the S3 storage.
 * Started once per test JVM and removed by Testcontainers when it ends. Needs Docker running.
 */
public final class S3TestServer {

    public static final String IMAGE = "chrislusf/seaweedfs:4.55";

    @SuppressWarnings("resource") // Closed by Testcontainers' Ryuk when the test JVM ends
    private static final GenericContainer<?> CONTAINER = new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withCommand("server", "-s3", "-dir=/data")
            .withExposedPorts(8333)
            .waitingFor(Wait.forHttp("/").forPort(8333).forStatusCode(200));

    private S3TestServer() {
    }

    /**
     * URL of the S3 API, e.g. {@code http://localhost:49153}.
     */
    public static synchronized String endpoint() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
        return "http://" + CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(8333);
    }

    public static S3Client client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint()))
                .forcePathStyle(true)
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
    }
}
