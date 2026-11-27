package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import com.tecnocampus.LS2.protube_back.services.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the Redis cache of GET /api/videos (CacheConfig) against a throwaway Redis in Docker. To prove a page comes
 * from the cache, videos are written straight to the repository, which does not remove the cached pages; the
 * services that change videos must remove them. Not {@code @Transactional}: the pages are removed after the commit,
 * as in a real request, so the data is deleted after each test instead.
 */
@SpringBootTest({
        "pro_tube.store.dir=c:",
        "pro_tube.load_initial_data=false",
        "spring.cache.type=redis",
        "pro_tube.cache.ttl-minutes=10"
})
@AutoConfigureMockMvc
class VideoPagesCacheTest {

    @SuppressWarnings("resource") // Closed by Testcontainers' Ryuk when the test JVM ends
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        REDIS.start();
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    VideoRepository videoRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    StringRedisTemplate redis;

    Long aliceVideoId;

    @BeforeEach
    void setUp() {
        User alice = userRepository.save(new User("alice", "alice@example.com", passwordEncoder.encode("password123")));
        aliceVideoId = videoRepository.save(video("cache-test-1", "Beach day", alice, 1700000000L)).getId();
    }

    @AfterEach
    void cleanUp() {
        videoRepository.deleteAll();
        userRepository.deleteAll();
        redis.delete(cachedKeys());
    }

    private static Video video(String storeId, String title, User uploader, long publishedAt) {
        Video video = new Video(storeId, title, null, uploader == null ? "Channel" : uploader.getUsername(), 60,
                1280, 720, Instant.ofEpochSecond(publishedAt), storeId + ".mp4", storeId + ".webp");
        video.setUploader(uploader);
        return video;
    }

    /**
     * Adds a video without going through a service, so the cached pages are not removed.
     */
    private void addVideoBehindTheCache(String title) {
        videoRepository.save(video("cache-test-2", title, null, 1800000000L));
    }

    private Set<String> cachedKeys() {
        return redis.keys("protube:videoPages::*");
    }

    private String bearer(String username) {
        return "Bearer " + jwtService.generateToken(username);
    }

    @Test
    void theListIsServedFromRedisUntilAVideoIsEdited() throws Exception {
        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        assertEquals(Set.of("protube:videoPages::0:12:"), cachedKeys());

        addVideoBehindTheCache("Mountain trip");

        // Same page read back from Redis: still one video, with every field of the DTO
        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Beach day"))
                .andExpect(jsonPath("$.content[0].author").value("alice"))
                .andExpect(jsonPath("$.content[0].publishedAt").value("2023-11-14T22:13:20Z"))
                .andExpect(jsonPath("$.content[0].thumbnailUrl").value("/media/cache-test-1.webp"));

        mockMvc.perform(put("/api/videos/" + aliceVideoId)
                        .header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Sunny beach\", \"tags\": [], \"ageRestricted\": false}"))
                .andExpect(status().isOk());
        assertTrue(cachedKeys().isEmpty());

        mockMvc.perform(get("/api/videos"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Mountain trip"))
                .andExpect(jsonPath("$.content[1].title").value("Sunny beach"));
    }

    @Test
    void aRejectedEditKeepsTheCachedPages() throws Exception {
        userRepository.save(new User("bob", "bob@example.com", "hash"));
        mockMvc.perform(get("/api/videos")).andExpect(status().isOk());

        mockMvc.perform(put("/api/videos/" + aliceVideoId)
                        .header("Authorization", bearer("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Not mine\", \"tags\": [], \"ageRestricted\": false}"))
                .andExpect(status().isForbidden());

        assertEquals(1, cachedKeys().size());
    }

    @Test
    void searchResultsAreCachedPerTrimmedQueryPageAndSize() throws Exception {
        mockMvc.perform(get("/api/videos").param("q", "beach").param("size", "5"))
                .andExpect(jsonPath("$.totalElements").value(1));
        addVideoBehindTheCache("Beach at night");

        // Same key once trimmed: the cached result
        mockMvc.perform(get("/api/videos").param("q", "  beach ").param("size", "5"))
                .andExpect(jsonPath("$.totalElements").value(1));
        // Another size is another key: read from the database
        mockMvc.perform(get("/api/videos").param("q", "beach").param("size", "6"))
                .andExpect(jsonPath("$.totalElements").value(2));

        assertEquals(Set.of("protube:videoPages::0:5:beach", "protube:videoPages::0:6:beach"), cachedKeys());
        long ttl = redis.getExpire("protube:videoPages::0:5:beach");
        assertTrue(ttl > 0 && ttl <= 600, "TTL in seconds: " + ttl);
    }

    @Test
    void renamingTheUploaderRemovesTheCachedPages() throws Exception {
        mockMvc.perform(get("/api/videos")).andExpect(jsonPath("$.content[0].author").value("alice"));

        mockMvc.perform(put("/api/account/username")
                        .header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"alicia\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/videos")).andExpect(jsonPath("$.content[0].author").value("alicia"));
    }

    @Test
    void deletingTheAccountRemovesTheCachedPages() throws Exception {
        mockMvc.perform(get("/api/videos")).andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/api/account")
                        .header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"password123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/videos")).andExpect(jsonPath("$.totalElements").value(0));
    }
}
