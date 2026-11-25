# Accessibility checks with axe

**Approved on:** PENDING — proposed in the PR of branch `task/66-axe-a11y`
**Approved by:** PENDING team review
**Used for:** README improvement "Accessibility (a11y)" (#66): automated WCAG 2.1 AA checks with axe in the E2E
suite, and fixing the violations they find in the frontend

## Prompt

The prompt was written in Catalan and is kept verbatim below (NFR-07 asks to preserve the exact prompt). It came after
reviewing which README improvements were done, where accessibility was only partial because nothing tested it.
Summary in English: "Do Swagger, the 90% threshold, axe".

```text
Fes Swagger, el llindar del 90%, axe
```

## Notes

* The same prompt produced three PRs: Swagger (#64), the 90% coverage threshold (#65) and this one (#66), each on its
  own branch and git worktree from an up-to-date `develop`, following `AGENTS.md`, `frontend/AGENTS.md` and
  `e2e/AGENTS.md`.
* `e2e/tests/accessibility.spec.ts` runs axe with the tags `wcag2a`, `wcag2aa`, `wcag21a` and `wcag21aa` on the home,
  search results, video detail, login, register, upload, settings, favourites, subscriptions, analytics and profile
  pages (public view and the owner's edit form), in the light and dark themes. Soft assertions, so one run lists the
  violations of every page with the elements and the contrast ratio found.
* Violations found and fixed:
  * `color-contrast`: white text on the brand red `#E63946` (4.17:1) on every primary button and badge; the red used
    as text (links, "Tube" of the logo, active settings options, the password toggle) on both themes; the secondary
    text `#6C757D` of the light theme (4.44:1); the outline danger buttons on the dark theme (2.94:1). Fixed with new
    and adjusted tokens in `theme.css` (`--pt-primary`, `--pt-primary-text`, `--pt-danger-text`, light
    `--pt-text-secondary`), documented in `docs/DESIGN_SYSTEM.md`. The logo files keep the brand red.
  * `label-content-name-mismatch`: the language button showed "EN" but its name was only "Language: English"; it is
    now "Language: English, EN" (a parenthesis would not work: axe ignores parenthesised text).
* No rule is disabled. The checks run with reduced motion, so axe never reads a colour in the middle of a transition.
* Not checked: states that need extra steps (open menus and dialogs, error messages); add them to the spec when they
  change.
