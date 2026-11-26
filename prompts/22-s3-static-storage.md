# S3 static storage

**Approved on:** PENDING — proposed in the PR of branch `task/78-s3-storage`
**Approved by:** PENDING team review
**Used for:** the README improvement "Use a static server to save resources (Cloudinary, AWS S3, etc.)" (Jira HH-78):
videos, thumbnails and avatars kept in an S3 bucket instead of the local store folder

## Prompt

The prompt was written in Catalan and is kept verbatim below (NFR-07 asks to preserve the exact prompt). It was sent
with the Jira card pasted after it. Summary in English: "Do this item for me".

```text
fesme aquest punt

Infrastructure: S3 static storage
Store videos and thumbnails in a static/object storage (AWS S3, Cloudinary...) instead of the local store folder.
```

## Notes

* Done on `task/78-s3-storage`, from an up-to-date `develop`, following `AGENTS.md` and `backend/AGENTS.md`.
* Design decisions:
  * Strategy pattern: a `MediaStorage` interface with `LocalMediaStorage` (the old behaviour, still the default
    outside Docker and in the tests) and `S3MediaStorage` (AWS SDK v2), chosen with `ENV_PROTUBE_STORAGE`.
  * The API and the frontend are unchanged: the URLs stay `/media/<file>` and, with S3, the backend redirects them to
    the public bucket, so the browser streams the files from the storage and not through Spring.
  * `compose.yaml` runs SeaweedFS as the S3 server, because the MinIO images can no longer be pulled from Docker Hub
    or quay.io without logging in. nginx exposes the bucket read-only under `/storage/`.
  * The importer copies the videoGrabber store into the bucket on startup (only missing files), so a new bucket is
    filled without extra steps.
* Not done: deleting the files of a single video (there is no "delete video" feature yet) and signed/private URLs:
  the files are public, as `/media/**` already was.
