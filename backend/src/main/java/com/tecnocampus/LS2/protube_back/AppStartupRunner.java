package com.tecnocampus.LS2.protube_back;

import com.tecnocampus.LS2.protube_back.services.VideoService;
import com.tecnocampus.LS2.protube_back.services.VideoStoreImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class AppStartupRunner implements ApplicationRunner {
    private static final Logger LOG =
            LoggerFactory.getLogger(AppStartupRunner.class);

    private final VideoStoreImporter videoStoreImporter;
    private final CacheManager cacheManager;
    private final Path rootPath;
    private final boolean loadInitialData;

    public AppStartupRunner(Environment env, VideoStoreImporter videoStoreImporter, CacheManager cacheManager) {
        this.videoStoreImporter = videoStoreImporter;
        this.cacheManager = cacheManager;
        final var rootDir = env.getProperty("pro_tube.store.dir");
        this.rootPath = Paths.get(rootDir);
        this.loadInitialData = env.getProperty("pro_tube.load_initial_data", Boolean.class, false);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (loadInitialData) {
            // Idempotent: videos already in the database are skipped, so it is safe on every startup
            videoStoreImporter.importFrom(rootPath);
        } else {
            LOG.info("pro_tube.load_initial_data is false: the video store is not imported");
        }
        clearVideoPages();
    }

    /**
     * Redis keeps its data when the backend restarts, so the cached pages may predate the videos just imported or a
     * database that was emptied: they are removed once the import is done. If Redis is down the app starts anyway.
     */
    private void clearVideoPages() {
        Cache videoPages = cacheManager.getCache(VideoService.VIDEO_PAGES_CACHE);
        if (videoPages == null) {
            return;
        }
        try {
            videoPages.clear();
        } catch (RuntimeException e) {
            LOG.warn("Could not clear the cache of video pages: {}", e.getMessage());
        }
    }
}
