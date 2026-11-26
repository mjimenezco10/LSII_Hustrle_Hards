# Architecture Decision Records

Short records of the design decisions of ProTube: the context, the decision, the alternatives that were discarded and
the consequences. One file per decision, numbered in order; a decision that changes later gets a new ADR that
supersedes the old one instead of editing it.

| ADR | Pattern | Where |
|---|---|---|
| [0001](./0001-strategy-media-storage.md) | Strategy: local or S3 media storage | `storage/MediaStorage` |
| [0002](./0002-static-factory-dtos.md) | Static factory methods for the response DTOs | `controller/dto/*Response.from/of` |
| [0003](./0003-observer-media-cleanup.md) | Observer: delete unused media files when the transaction ends | `storage/MediaCleanupEvent`, `MediaCleanupListener` |

Template: **Status** · **Context** · **Decision** · **Alternatives considered** · **Consequences** · **Tests**.
