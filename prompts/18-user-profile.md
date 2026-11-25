# User profile (US-03)

**Approved on:** PENDING — proposed in the PR of branch `task/3-user-profile`
**Approved by:** PENDING team review
**Used for:** implementing US-03 "View and edit profile" of `REQUIREMENT.md` (#3): public profile page of every user
and channel with its videos, and the owner editing the avatar, description, location and website

## Prompt

The prompt was written in Catalan and is kept verbatim below (NFR-07 asks to preserve the exact prompt). It came after
an audit of the MVP against `REQUIREMENT.md`, which found US-03 was the only Must-have story not delivered. Summary in
English: "Yes, open the Sonar PR and start with US-03".

```text
Sí, fes el PR del sonar i comença amb la US-03
```

## Notes

* Done on `task/3-user-profile`, from an up-to-date `develop`, in its own git worktree, following `AGENTS.md`,
  `backend/AGENTS.md` (with the `create-rest-endpoint` skill), `frontend/AGENTS.md` and `e2e/AGENTS.md`.
* The backend (endpoints, service, entity fields, messages in en/es/ca, tests) was written by an AI subagent against
  an API contract fixed beforehand; the frontend and the E2E tests in parallel against the same contract.
* Design decisions: a profile exists for every registered user and also for every channel of the video store
  (`registered: false`, name and videos only), so every video author links to a profile and the Subscribe button of
  US-13 is on profiles too. The email is never public. Avatars are saved in `<store>/avatars/` and served through
  `/media/**`; replaced or removed images are deleted only after the change is committed.
* Not done: the "Public profile" toggle of Settings still does nothing (every profile is public), and the navbar still
  shows the initial, not the avatar.
* Number 18 because 17 is used twice already.
