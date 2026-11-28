# ADR 0003 — Observer: delete unused media files when the transaction ends

**Status:** Accepted (Jira HH-93)

## Context

Files and database rows cannot be changed atomically. When a transaction replaces or removes a media file:

* the **old** file (old avatar, files of a deleted account) may only be deleted **after the commit**: if the
  transaction rolls back, the database still points to it;
* a **new** file already written (new avatar) must be deleted **if the transaction rolls back**, or it stays in the
  storage forever.

`ProfileService` and `AccountDeletionService` each solved this with their own copy of hand-written
`TransactionSynchronizationManager` code (`afterTransaction`, `afterCommit`), mixing transaction plumbing with
business logic, and their unit tests had to drive Spring's synchronisation by hand.

## Decision

Apply the **Observer** pattern with Spring application events:

* **Event**: `MediaCleanupEvent(deleteOnCommit, deleteOnRollback)`, a record with the keys of the files that become
  useless in each case.
* **Publishers (subjects)**: the services call `ApplicationEventPublisher.publishEvent(...)` and do not know who
  deletes the files, or when.
* **Observer**: `MediaCleanupListener`, with two `@TransactionalEventListener` methods: `AFTER_COMMIT` deletes
  `deleteOnCommit` and `AFTER_ROLLBACK` deletes `deleteOnRollback`. Published outside a transaction
  (`fallbackExecution = true`) the change is already final, so it counts as committed.

## Alternatives considered

* **Keep the `TransactionSynchronization` helpers** in each service: duplicated, and easy to get wrong in the next
  service that touches files.
* **Delete the files directly** at the end of the service method: a later rollback would leave rows pointing to
  missing files.
* **Observer for analytics** (an event per video view, as suggested in the Jira card): a view is already a single
  `INSERT` and the analytics are computed with queries, so the pattern would add indirection without solving a
  problem. The cleanup case has a real need: the reaction must wait for the outcome of the transaction.
* **A scheduled job that deletes orphan files**: catches any leak, but has to list the whole storage and compare it
  with the database; heavier than reacting to the change that created the orphan.

## Consequences

* The services only describe *what* became useless; *when* it is deleted (commit or rollback) lives in one listener.
* New observers (e.g. logging or metrics of freed space) can listen to the same event without touching the services.
* The listener runs synchronously after the transaction, so a slow storage delays the response a little; it could
  become `@Async` later. Failed deletions are only logged (`MediaStorage.deleteQuietly`): an orphan file wastes space
  but never breaks the app.
* `VideoUploadService` keeps its own `try/catch` cleanup, because it deletes its new files as soon as the upload fails,
  inside the method; it could move to the same event in the future.

## Tests

`MediaCleanupListenerTest` (real Spring transactions: commit, rollback, no transaction, null keys),
`ProfileServiceTest` and `AccountDeletionServiceTest` (the events each change publishes, and the files deleted when the
change counts as committed).
