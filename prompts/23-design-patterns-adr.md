# Documented design patterns (ADRs)

**Approved on:** PENDING — proposed in the PR of branch `task/93-design-patterns`
**Approved by:** PENDING team review
**Used for:** the README improvement "Use of design patterns" (Jira HH-93): an Observer for the cleanup of unused
media files, and one ADR per applied pattern (Strategy, static factory methods, Observer) in `docs/adr/`

## Prompt

The prompts were written in Catalan and Spanish and are kept verbatim below (NFR-07 asks to preserve the exact
prompt). The first one came with the Jira card pasted after it; the second one answered the choice between two
Observer designs (media file cleanup or analytics). Summary in English: "Shall we do this one?" and "File cleanup".

```text
fem aquest ?

Documented design patterns
Apply and document design patterns in the codebase.
Scope: e.g. Strategy for storage (local or S3), Builder/Factory for DTOs, Observer for statistics/analytics,
each pattern justified in an ADR or in Confluence.
Acceptance criteria: each applied pattern is implemented and covered by tests; each pattern has an ADR / Confluence
page explaining why it was chosen.
```

```text
Limpieza de ficheros
```

## Notes

* Done on `task/93-design-patterns`, from an up-to-date `develop`, following `AGENTS.md` and `backend/AGENTS.md`.
* Strategy (storage, HH-78) and the DTO factories already existed: only their ADRs and a focused `DtoFactoriesTest`
  were added. Builder was discarded and the ADR explains why (immutable records built in one go).
* Observer replaces the hand-written `TransactionSynchronizationManager` code of `ProfileService` and
  `AccountDeletionService` with `MediaCleanupEvent` + `MediaCleanupListener` (`@TransactionalEventListener`
  after commit / after rollback). The analytics Observer suggested by the card was discarded (see ADR 0003).
* ADRs live in the repository (`docs/adr/`) because the team does not use Confluence yet.
