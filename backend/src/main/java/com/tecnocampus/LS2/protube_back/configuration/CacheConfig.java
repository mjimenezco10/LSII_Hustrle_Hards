package com.tecnocampus.LS2.protube_back.configuration;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.controller.dto.PageResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.VideoSummaryResponse;
import com.tecnocampus.LS2.protube_back.services.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/**
 * Cache of the pages of GET /api/videos ({@link VideoService#VIDEO_PAGES_CACHE}). The cache manager is the one of
 * {@code spring.cache.type} ({@code ENV_PROTUBE_CACHE}): {@code none} (default, nothing is cached) or {@code redis}.
 * <p>
 * The caching advice runs outside the transaction (lower {@code order} than {@code @Transactional}): a page is
 * removed after the change that makes it old is committed, so a request in between cannot put the old page back, and a
 * cache hit does not open a database transaction.
 */
@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig implements CachingConfigurer {

    public static final String KEY_PREFIX = "protube:";

    /**
     * The cache is optional: if Redis cannot be read or written, the error is logged and the request reads the
     * database, as without cache.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    /**
     * Pages are stored as JSON of their exact type ({@code PageResponse<VideoSummaryResponse>}), with the same
     * ObjectMapper as the REST responses, so no class name is written in Redis. Keys look like
     * {@code protube:videoPages::0:12:beach} (page, size and search text).
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer videoPagesCacheCustomizer(
            ObjectMapper objectMapper, @Value("${pro_tube.cache.ttl-minutes}") long ttlMinutes) {
        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponse.class, VideoSummaryResponse.class);
        RedisCacheConfiguration videoPages = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlMinutes))
                .prefixCacheNameWith(KEY_PREFIX)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, pageType)));
        return builder -> builder.withCacheConfiguration(VideoService.VIDEO_PAGES_CACHE, videoPages);
    }
}
