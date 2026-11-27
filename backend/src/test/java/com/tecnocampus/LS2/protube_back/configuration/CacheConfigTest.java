package com.tecnocampus.LS2.protube_back.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tecnocampus.LS2.protube_back.controller.dto.PageResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.VideoSummaryResponse;
import com.tecnocampus.LS2.protube_back.services.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class CacheConfigTest {

    private final CacheConfig config = new CacheConfig();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private RedisCacheConfiguration videoPagesConfiguration() {
        RedisCacheManagerBuilder builder = RedisCacheManagerBuilder.fromConnectionFactory(
                mock(RedisConnectionFactory.class));
        config.videoPagesCacheCustomizer(objectMapper, 5).customize(builder);
        return builder.getCacheConfigurationFor(VideoService.VIDEO_PAGES_CACHE).orElseThrow();
    }

    @Test
    void redisErrorsAreLoggedInsteadOfFailingTheRequest() {
        assertInstanceOf(LoggingCacheErrorHandler.class, config.errorHandler());
    }

    @Test
    void videoPagesExpireAfterTheConfiguredMinutesAndUseTheProtubePrefix() {
        RedisCacheConfiguration videoPages = videoPagesConfiguration();

        assertEquals(Duration.ofMinutes(5), videoPages.getTtlFunction().getTimeToLive(null, null));
        assertEquals("protube:videoPages::", videoPages.getKeyPrefixFor(VideoService.VIDEO_PAGES_CACHE));
        assertFalse(videoPages.getAllowCacheNullValues());
    }

    @Test
    void aPageIsStoredAsJsonAndReadBackWithItsType() {
        RedisCacheConfiguration videoPages = videoPagesConfiguration();
        PageResponse<VideoSummaryResponse> page = new PageResponse<>(List.of(new VideoSummaryResponse(1L, "Beach",
                "alice", 60, Instant.parse("2026-11-27T10:00:00Z"), "/media/1.webp", "/media/1.mp4", false)),
                0, 12, 1, 1);

        ByteBuffer json = videoPages.getValueSerializationPair().write(page);
        Object read = videoPages.getValueSerializationPair().read(json.duplicate());

        assertEquals(page, read);
        // Plain JSON of the DTO: no class names that would let Redis data choose what to instantiate
        String text = new String(json.array(), json.arrayOffset(), json.remaining());
        assertFalse(text.contains("com.tecnocampus"), text);
    }
}
