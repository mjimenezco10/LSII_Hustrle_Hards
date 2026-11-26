package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.storage.MediaStorage;
import com.tecnocampus.LS2.protube_back.storage.S3MediaStorage;
import com.tecnocampus.LS2.protube_back.storage.S3TestServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With {@code pro_tube.storage.type=s3} the app uses the bucket (created on startup) and {@code /media/**} redirects
 * the browser to it.
 */
@SpringBootTest({
        "pro_tube.store.dir=c:",
        "pro_tube.load_initial_data=false",
        "pro_tube.storage.type=s3",
        "pro_tube.storage.s3.bucket=redirect-test",
        "pro_tube.storage.s3.access-key=test",
        "pro_tube.storage.s3.secret-key=test",
        "pro_tube.storage.s3.public-url=/storage"
})
@AutoConfigureMockMvc
class MediaRedirectControllerTest {

    @DynamicPropertySource
    static void s3Endpoint(DynamicPropertyRegistry registry) {
        registry.add("pro_tube.storage.s3.endpoint", S3TestServer::endpoint);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MediaStorage mediaStorage;

    @Autowired
    S3Client s3Client;

    @Test
    void usesTheBucketCreatedOnStartup() {
        assertInstanceOf(S3MediaStorage.class, mediaStorage);
        assertDoesNotThrow(() -> s3Client.headBucket(request -> request.bucket("redirect-test")));
    }

    @Test
    void redirectsAMediaFileToTheBucket() throws Exception {
        mockMvc.perform(get("/media/upload-1.mp4"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/storage/upload-1.mp4"));
    }

    @Test
    void keepsTheFolderOfTheKey() throws Exception {
        mockMvc.perform(get("/media/avatars/a.webp"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/storage/avatars/a.webp"));
    }

    @Test
    void rejectsKeysOutsideTheBucket() throws Exception {
        // Spring Security's firewall already rejects ".." (400) before the controller checks it
        mockMvc.perform(get("/media/a/../../other-bucket/x.mp4"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void rejectsAnEmptyKey() throws Exception {
        mockMvc.perform(get("/media/"))
                .andExpect(status().isNotFound());
    }
}
