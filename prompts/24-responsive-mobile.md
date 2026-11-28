# Responsive / mobile layout

**Approved on:** PENDING — proposed in the PR of branch `task/91-responsive`
**Approved by:** PENDING team review
**Used for:** the README improvement "Mobile/Responsiveness" (Jira HH-91, US-14): the grid, the player, the user
menu and the forms on phones and tablets

## Prompt

The prompt was written in Spanish and is kept verbatim below (NFR-07 asks to preserve the exact prompt). It was sent
with the Jira card pasted in the middle. Summary in English: "OK, the pull request is done, let's go with this now
[card]. Responsive / mobile is HH91".

```text
vale ya esta la pull request vamos con esto ahora

Descripción

As a user, I want to use ProTube on my phone or tablet, so that I can watch videos anywhere.

Scope
Video grid

Video player

User menu

Forms

Acceptance criteria
Grid, player, user menu and forms adapt correctly to mobile and tablet breakpoints.

No horizontal scroll or broken layouts on common mobile widths

Responsive / mobile es el HH91
```

## Notes

* Done on `task/91-responsive`, from an up-to-date `develop`, following `AGENTS.md` and `frontend/AGENTS.md`.
* Approach: measure first. A throwaway Playwright script opened every page at 320, 360, 390, 414, 768 and 1024px,
  reported the elements that stick out of the viewport and took screenshots. Most of the app was already responsive
  (Bootstrap grid); the problems found were:
  * Navbar: at 360px logged out and at 320px on every page, "Log in" wrapped and the actions overflowed onto the logo.
    Fixed with a logo-only brand below 400px, `white-space: nowrap` and tighter padding.
  * Settings: the "Saved on this device" badge broke inside its pill and the section links were cut behind a hidden
    sideways scroll. Both wrap now.
  * Analytics: the per-video table scrolled sideways and hid the last columns. On phones each row is a block with
    its values labelled (`data-label`, translated).
* Tests: `e2e/tests/responsive.spec.ts` (phone, 320px phone and tablet) checks the golden path of the grid, player,
  user menu and a form, and that no page scrolls sideways; three of its tests fail without the CSS changes. The
  phone layout was added to `accessibility.spec.ts`. Jest checks the `data-label` of the analytics cells.
* Documented in `docs/DESIGN_SYSTEM.md` ("Responsive") and as US-14 in `REQUIREMENT.md`.
