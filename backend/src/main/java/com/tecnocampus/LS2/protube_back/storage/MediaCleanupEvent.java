package com.tecnocampus.LS2.protube_back.storage;

import java.util.List;

/**
 * Published by a service when a transaction leaves media files unused (Observer pattern, see
 * docs/adr/0003-observer-media-cleanup.md). {@link MediaCleanupListener} deletes them once the transaction ends:
 * {@code deleteOnCommit} if it commits (e.g. the old avatar) and {@code deleteOnRollback} if it rolls back (e.g. the
 * new avatar that will never be used). The publisher does not need to know how or when files are deleted.
 *
 * @param deleteOnCommit   keys of the files that are no longer used once the change is committed
 * @param deleteOnRollback keys of the files written by the change, useless if it is rolled back
 */
public record MediaCleanupEvent(List<String> deleteOnCommit, List<String> deleteOnRollback) {

    public MediaCleanupEvent {
        // Nulls are skipped, so callers can pass "the old avatar" even when there was none
        deleteOnCommit = deleteOnCommit.stream().filter(key -> key != null).toList();
        deleteOnRollback = deleteOnRollback.stream().filter(key -> key != null).toList();
    }

    /**
     * Files that are no longer used once the change is committed; nothing to undo on rollback.
     */
    public static MediaCleanupEvent onCommit(List<String> keys) {
        return new MediaCleanupEvent(keys, List.of());
    }
}
