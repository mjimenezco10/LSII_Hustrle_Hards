# Coverage threshold of 90%

**Approved on:** PENDING — proposed in the PR of branch `task/65-coverage-90`
**Approved by:** PENDING team review
**Used for:** enforcing the README improvement "More than 90% of test coverage" (#65): raising the JaCoCo and Jest
thresholds to 90% so CI fails if the coverage of the backend or the frontend drops below it

## Prompt

The prompt was written in Catalan and is kept verbatim below (NFR-07 asks to preserve the exact prompt). It came after
a review of the README improvements already delivered, which found that both apps were above 90% coverage but nothing
kept them there. Summary in English: "Do Swagger, the 90% threshold and axe".

```text
Fes Swagger, el llindar del 90%, axe
```

## Notes

* Done on `task/65-coverage-90`, from an up-to-date `develop`, in its own git worktree, following `AGENTS.md`.
* The same prompt produced three PRs, one per issue: Swagger (#64), this coverage threshold (#65) and the axe
  accessibility checks (#66).
* Coverage measured before raising the thresholds, on `develop` with US-03 merged:
  * Backend (JaCoCo): instructions 99.1%, branches 94.8%, lines 98.5%, methods 98.0%.
  * Frontend (Jest): statements 98.3%, branches 92.0%, functions 97.8%, lines 99.1%.
* Every metric was already above 90%, so no tests were added. Branches are the closest to the limit on both apps.
* The per-class rule of the backend stays at 50% of lines: a 90% minimum per class would fail on small entities such
  as `CommentLike` (78%), whose uncovered lines are JPA constructors and getters.
* Number 20 because 19 is used by the Swagger PR (#64) of the same prompt.
