# ADR 0002 — Static factory methods for the response DTOs

**Status:** Accepted

## Context

The controllers never return JPA entities (`backend/AGENTS.md`): every response is a Java `record` in
`controller/dto/`. Turning an entity into its response is not a plain field copy: file names become `/media/<file>`
URLs, replies are nested inside their comment, `likedByMe` depends on who is asking, imported videos have no
uploader… That logic is needed by several services (`VideoService`, `CommentService`, `FavouriteService`,
`ProfileService`, `VideoUploadService`).

## Decision

Each response record has **static factory methods** (a simple form of the Factory pattern) that build it from the
domain object:

* `VideoSummaryResponse.from(Video)`, `VideoDetailResponse.from(Video)` / `from(Video, likedCommentIds)`
* `CommentResponse.from(Comment)` / `from(Comment, likedCommentIds)`
* `ProfileResponse.of(User, videoCount)`, `UserResponse.from(User)`, `VideoAnalyticsResponse.from(VideoStatistics)`

The overloads name the variants (seen by an anonymous visitor, or by a viewer who likes some comments) and leave the
canonical constructor of the record for the fields only.

## Alternatives considered

* **Builder**: useful for objects with many optional fields that are built step by step. Our responses are immutable
  records, always built from one entity in one go, so a builder would add a class per DTO without making anything
  clearer.
* **A mapping library (MapStruct, ModelMapper)**: generated or reflective code for mappings that are mostly custom
  (URLs, nesting, flags), and one more dependency to learn and debug.
* **Mapping in the services**: the same conversion (e.g. the `/media/` prefix) would be repeated in several services.

## Consequences

* One place per response decides how an entity is exposed: changing the media URLs touches only the factories.
* The factories are plain functions without Spring, so they are tested without a context.
* A factory depends on the getters of the entity, so a domain change may require updating the DTO (caught at
  compile time).

## Tests

`DtoFactoriesTest` (media URLs, imported videos without uploader, comments that cannot be liked yet, profiles without
avatar), plus every controller test, which checks the JSON they produce.
