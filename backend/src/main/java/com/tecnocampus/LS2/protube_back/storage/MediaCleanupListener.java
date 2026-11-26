package com.tecnocampus.LS2.protube_back.storage;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer of {@link MediaCleanupEvent}: deletes the media files a transaction left unused, only once the
 * transaction has ended, so a rollback never points the database to a file that is already gone.
 */
@Component
public class MediaCleanupListener {

    private final MediaStorage mediaStorage;

    public MediaCleanupListener(MediaStorage mediaStorage) {
        this.mediaStorage = mediaStorage;
    }

    /**
     * After the commit. Published outside a transaction ({@code fallbackExecution}) the change is already final, so
     * it counts as committed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCommit(MediaCleanupEvent event) {
        event.deleteOnCommit().forEach(mediaStorage::deleteQuietly);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRollback(MediaCleanupEvent event) {
        event.deleteOnRollback().forEach(mediaStorage::deleteQuietly);
    }
}
