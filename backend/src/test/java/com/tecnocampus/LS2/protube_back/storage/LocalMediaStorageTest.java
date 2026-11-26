package com.tecnocampus.LS2.protube_back.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMediaStorageTest {

    private static final byte[] BYTES = "bytes".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path store;

    @Test
    void savesTheFileInTheStoreCreatingItsFolder() throws IOException {
        new LocalMediaStorage(store).save("avatars/a.webp", new MockMultipartFile("avatar", "a.webp", "image/webp", BYTES));

        assertArrayEquals(BYTES, Files.readAllBytes(store.resolve("avatars/a.webp")));
    }

    @Test
    void importingLeavesTheFileOfTheStoreWhereItIs() throws IOException {
        Path file = Files.write(store.resolve("abc.mp4"), BYTES);

        new LocalMediaStorage(store).importFile("abc.mp4", file);

        assertArrayEquals(BYTES, Files.readAllBytes(file));
    }

    @Test
    void deletesTheFileAndIgnoresMissingOnes() throws IOException {
        Files.write(store.resolve("abc.mp4"), BYTES);
        LocalMediaStorage storage = new LocalMediaStorage(store);

        storage.deleteQuietly("abc.mp4");

        assertFalse(Files.exists(store.resolve("abc.mp4")));
        assertDoesNotThrow(() -> storage.deleteQuietly("missing.mp4"));
    }

    @Test
    void aFileThatCannotBeDeletedIsOnlyLogged() throws IOException {
        // A folder that is not empty cannot be deleted as a file
        Files.createDirectories(store.resolve("avatars"));
        Files.write(store.resolve("avatars/a.webp"), BYTES);

        assertDoesNotThrow(() -> new LocalMediaStorage(store).deleteQuietly("avatars"));
        assertTrue(Files.exists(store.resolve("avatars/a.webp")));
    }
}
