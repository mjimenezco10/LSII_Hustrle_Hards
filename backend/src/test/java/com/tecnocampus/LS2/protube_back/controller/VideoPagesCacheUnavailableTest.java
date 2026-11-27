package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With the Redis cache enabled but Redis unreachable (nothing listens on port 1), the app still starts and the video
 * list is read from the database (CacheConfig#errorHandler).
 */
@SpringBootTest({
        "pro_tube.store.dir=c:",
        "pro_tube.load_initial_data=false",
        "spring.cache.type=redis",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=1"
})
@AutoConfigureMockMvc
@Transactional
class VideoPagesCacheUnavailableTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    VideoRepository videoRepository;

    @Test
    void theListIsReadFromTheDatabaseWhenRedisIsDown() throws Exception {
        videoRepository.save(new Video("down-1", "Still works", null, "Channel", 60, 1280, 720,
                Instant.ofEpochSecond(1700000000L), "down-1.mp4", "down-1.webp"));

        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Still works"));
    }
}
