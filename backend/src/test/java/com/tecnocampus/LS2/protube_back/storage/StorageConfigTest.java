package com.tecnocampus.LS2.protube_back.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StorageConfigTest {

    private final StorageConfig config = new StorageConfig();

    @Test
    void localStorageUsesTheStoreFolder() {
        assertInstanceOf(LocalMediaStorage.class, config.localMediaStorage("store/"));
    }

    @Test
    void withoutEndpointNorKeysTheClientTalksToAwsWithTheDefaultCredentials() {
        try (S3Client client = config.s3Client("", "eu-west-1", "", "")) {
            assertEquals("eu-west-1", client.serviceClientConfiguration().region().id());
        }
    }

    @Test
    void withAnEndpointTheClientTalksToThatServer() {
        try (S3Client client = config.s3Client("http://s3:8333", "us-east-1", "key", "secret")) {
            assertEquals("http://s3:8333", client.serviceClientConfiguration().endpointOverride().orElseThrow().toString());
        }
    }
}
