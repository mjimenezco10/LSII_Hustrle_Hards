# Redis cache

**Approved on:** PENDING — proposed in the PR of branch `task/77-redis-cache`
**Approved by:** PENDING team review
**Used for:** the README improvement "Use of a caching system (Redis, Memcached, etc.)" (Jira HH-77):
the video list and the search results cached in Redis

## Prompt

The board card was pasted as is, with no other text (NFR-07 asks to preserve the exact prompt):

```text
Infrastructure: Redis cache

Tareas por hacer

Agentes


Mejorar el tipo de actividad Feature


Descripción

Add a caching layer with Redis (e.g. video list, search results).
```

## Notes

* Done on `task/77-redis-cache`, from an up-to-date `develop`, following `AGENTS.md` and `backend/AGENTS.md`.
* Design decisions:
  * Spring Cache abstraction (`@Cacheable` / `@CacheEvict`) over Spring Data Redis, so the services do not know about
    Redis. `spring.cache.type` (`ENV_PROTUBE_CACHE`) is `none` by default (local runs, tests, E2E) and `redis` in
    `compose.yaml`, the same pattern as `ENV_PROTUBE_STORAGE`.
  * One cache, `videoPages`: `VideoService.searchVideos` is the only entry point of `GET /api/videos`, and a blank
    query returns the list, so list and search share it. Key: page, size and trimmed query.
  * Invalidation instead of a short TTL: every service that changes what a page shows (upload, edit, username change,
    account deletion) evicts all the pages. The caching advice is ordered outside `@Transactional`, so the eviction
    happens after the commit. The pages are also cleared on startup (Redis outlives the backend) and expire after 10
    minutes as a safety net.
  * Values stored as typed JSON (`Jackson2JsonRedisSerializer` of `PageResponse<VideoSummaryResponse>`) instead of
    Java serialization or default typing: readable in `redis-cli`, and Redis data cannot choose a class to build.
  * `LoggingCacheErrorHandler`: Redis down means no cache, not a broken app.
* Not cached: the video detail (it has the comments and the likes of each viewer) and per-user lists.
* No frontend or E2E change: the API answers the same; the E2E suite runs without cache.
