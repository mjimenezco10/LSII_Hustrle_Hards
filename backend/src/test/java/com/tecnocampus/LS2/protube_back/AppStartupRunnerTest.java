package com.tecnocampus.LS2.protube_back;

import com.tecnocampus.LS2.protube_back.services.VideoService;
import com.tecnocampus.LS2.protube_back.services.VideoStoreImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppStartupRunnerTest {

    @Mock
    VideoStoreImporter videoStoreImporter;

    @Mock
    CacheManager cacheManager;

    @Mock
    Cache videoPages;

    private AppStartupRunner runner(String loadInitialData) {
        MockEnvironment env = new MockEnvironment()
                .withProperty("pro_tube.store.dir", "store")
                .withProperty("pro_tube.load_initial_data", loadInitialData);
        return new AppStartupRunner(env, videoStoreImporter, cacheManager);
    }

    @Test
    void importsTheStoreAndThenClearsTheCachedPages() {
        when(cacheManager.getCache(VideoService.VIDEO_PAGES_CACHE)).thenReturn(videoPages);

        runner("true").run(null);

        InOrder order = inOrder(videoStoreImporter, videoPages);
        order.verify(videoStoreImporter).importFrom(Paths.get("store"));
        order.verify(videoPages).clear();
    }

    @Test
    void doesNotImportWhenInitialDataIsDisabledButStillClearsTheCachedPages() {
        when(cacheManager.getCache(VideoService.VIDEO_PAGES_CACHE)).thenReturn(videoPages);

        runner("false").run(null);

        verify(videoStoreImporter, never()).importFrom(any());
        verify(videoPages).clear();
    }

    @Test
    void startsEvenIfTheCacheCannotBeCleared() {
        when(cacheManager.getCache(VideoService.VIDEO_PAGES_CACHE)).thenReturn(videoPages);
        doThrow(new RedisConnectionFailureException("Redis is down")).when(videoPages).clear();

        assertDoesNotThrow(() -> runner("false").run(null));
    }

    @Test
    void startsWithACacheManagerThatHasNoSuchCache() {
        assertDoesNotThrow(() -> runner("false").run(null));
    }
}
