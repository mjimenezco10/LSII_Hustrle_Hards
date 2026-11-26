# ADR 0001 — Strategy: local or S3 media storage

**Status:** Accepted (Jira HH-78, PR #71)

## Context

Videos, thumbnails and avatars were written with `java.nio.file.Files` straight from three services
(`VideoUploadService`, `ProfileService`, `AccountDeletionService`) into the store folder. The README improvement
"Use a static server to save resources" asks to keep them in an object storage such as AWS S3, but development, the
tests and the E2E suite are simpler with a plain folder. Both ways of storing files must coexist, and the services
should not care which one is in use.

## Decision

Apply the **Strategy** pattern:

* `MediaStorage` is the strategy interface: `save(key, file)`, `importFile(key, path)` and `deleteQuietly(key)`.
* Two concrete strategies: `LocalMediaStorage` (the folder `pro_tube.store.dir`) and `S3MediaStorage` (a bucket,
  through the AWS SDK v2).
* `StorageConfig` chooses the strategy once, from `pro_tube.storage.type` (`ENV_PROTUBE_STORAGE`), with
  `@ConditionalOnProperty`. The services (the context) only receive a `MediaStorage` by constructor injection.

## Alternatives considered

* **`if (s3) … else …` in every service**: duplicates the choice in three places, and every new service that stores
  files would have to repeat it.
* **Only S3** (with a local S3 server for development and tests): every developer and the E2E suite would need the
  extra container; the folder is still the easiest setup for local work.
* **Spring's `Resource` abstraction**: good for reading, but has no portable write/delete API for S3.

## Consequences

* Adding a third storage (e.g. Cloudinary) is one new class plus one bean in `StorageConfig`; no service changes.
* The services are easier to unit-test: `LocalMediaStorage` on a `@TempDir`, or a Mockito mock of `MediaStorage`.
* The URLs of the API do not depend on the strategy (`/media/<key>`): with S3, `MediaRedirectController` redirects
  them to the bucket.

## Tests

`LocalMediaStorageTest`, `S3MediaStorageTest` (against a throwaway SeaweedFS with Testcontainers), `StorageConfigTest`,
`MediaRedirectControllerTest` (full context with `type=s3`), and the service tests, which run with
`LocalMediaStorage`.
