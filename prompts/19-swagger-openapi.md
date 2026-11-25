# Swagger / OpenAPI documentation

**Approved on:** PENDING — proposed in the PR of branch `task/64-swagger`
**Approved by:** PENDING team review
**Used for:** the README improvement "Documentation (Swagger, JSDocs etc.)" (#64): an OpenAPI description of the REST
API and a Swagger UI, with the JWT bearer authentication

## Prompt

The prompt was written in Catalan and is kept verbatim below (NFR-07 asks to preserve the exact prompt). It came after
a review of the README improvements already delivered, which found the documentation one was partial because there
was no Swagger. Summary in English: "Do Swagger, the 90% threshold and axe".

```text
Fes Swagger, el llindar del 90%, axe
```

## Notes

* Done on `task/64-swagger`, from an up-to-date `develop`, in its own git worktree, following `AGENTS.md` and
  `backend/AGENTS.md`.
* The same prompt also produced the PRs of #65 (coverage thresholds at 90%) and #66 (axe accessibility checks in the
  E2E suite), each on its own branch.
* Design decisions: springdoc 2.6 (the line for Spring Boot 3.3), only `/api/**` is documented (not the Thymeleaf
  pages), one tag per controller, and the JWT bearer scheme is declared for every operation so "Authorize" in Swagger
  UI works; public endpoints ignore the token. The docs are public in every profile, since the API itself is.
* Not done: per-operation `@Operation` summaries and response examples; the tags and the DTOs are enough to read and
  try the API.
