package com.tecnocampus.LS2.protube_back.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Observer pattern end to end: a {@link MediaCleanupEvent} published inside a real Spring transaction is handled by
 * {@link MediaCleanupListener} only when the transaction ends, and according to how it ends.
 */
@SpringBootTest({"pro_tube.load_initial_data=false"})
class MediaCleanupListenerTest {

    private static final Path STORE = createStore();

    @DynamicPropertySource
    static void store(DynamicPropertyRegistry registry) {
        registry.add("pro_tube.store.dir", () -> STORE + "/");
    }

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate transaction;

    private static Path createStore() {
        try {
            return Files.createTempDirectory("media-cleanup");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    void writeFiles() throws IOException {
        Files.writeString(STORE.resolve("old.webp"), "old");
        Files.writeString(STORE.resolve("new.webp"), "new");
    }

    private static MediaCleanupEvent replaceOldWithNew() {
        return new MediaCleanupEvent(List.of("old.webp"), List.of("new.webp"));
    }

    @Test
    void deletesTheUnusedFilesOnlyAfterTheCommit() {
        transaction.executeWithoutResult(status -> {
            events.publishEvent(replaceOldWithNew());
            // Still inside the transaction: nothing is deleted yet
            assertTrue(Files.exists(STORE.resolve("old.webp")));
        });

        assertFalse(Files.exists(STORE.resolve("old.webp")));
        assertTrue(Files.exists(STORE.resolve("new.webp")));
    }

    @Test
    void aRollbackDeletesTheNewFilesAndKeepsTheOldOnes() {
        transaction.executeWithoutResult(status -> {
            events.publishEvent(replaceOldWithNew());
            status.setRollbackOnly();
        });

        assertTrue(Files.exists(STORE.resolve("old.webp")));
        assertFalse(Files.exists(STORE.resolve("new.webp")));
    }

    @Test
    void outsideATransactionTheChangeCountsAsCommitted() {
        events.publishEvent(replaceOldWithNew());

        assertFalse(Files.exists(STORE.resolve("old.webp")));
        assertTrue(Files.exists(STORE.resolve("new.webp")));
    }

    @Test
    void nullKeysAreIgnored() {
        MediaCleanupEvent event = new MediaCleanupEvent(Arrays.asList(null, "a.webp"), List.of());

        assertEquals(List.of("a.webp"), event.deleteOnCommit());
    }
}
