package com.tecnocampus.LS2.protube_back.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Keeps the media files in a folder of the server ({@code pro_tube.store.dir}), served under {@code /media/**} by
 * {@code MvcConfig}. The key is the path of the file relative to that folder.
 */
public class LocalMediaStorage implements MediaStorage {

    private static final Logger LOG = LoggerFactory.getLogger(LocalMediaStorage.class);

    private final Path storeDir;

    public LocalMediaStorage(Path storeDir) {
        this.storeDir = storeDir;
    }

    @Override
    public void save(String key, MultipartFile file) throws IOException {
        Path target = storeDir.resolve(key);
        Files.createDirectories(target.getParent());
        file.transferTo(target);
    }

    /**
     * The video store is already this folder, so its files need no copy.
     */
    @Override
    public void importFile(String key, Path file) {
        // Nothing to do: the file is already where /media serves it from
    }

    @Override
    public void deleteQuietly(String key) {
        Path path = storeDir.resolve(key);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.warn("Could not delete {}", path, e);
        }
    }
}
