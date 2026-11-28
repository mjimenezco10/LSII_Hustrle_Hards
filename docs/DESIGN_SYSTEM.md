# ProTube design system

Visual rules for every screen of the frontend. The tokens live in
[`frontend/src/theme.css`](../frontend/src/theme.css): use them through `var(--pt-...)` and never hardcode a colour
in a component stylesheet. The UI is built on Bootstrap 5, so prefer Bootstrap classes and only add custom CSS when
Bootstrap has no equivalent.

## Brand

- **Logo (PT monogram):** a capital "P" rotated 180° (vertical stem on the right, bowl at the bottom-left; not a
  normal "P", a "b" or a "q") whose stem rises into a capital "T" with a horizontal crossbar on top. Stem
  thickness is 12% of the canvas and the corner radius is half the stem, so every end is round. Assets in
  `frontend/public/`:
  - `protube-logo.svg`: standalone icon, brand red on a transparent background (login page).
  - `protube-logo-mark.svg`: white monogram on a red 36px square with 8px radius (navbar lockup).
  - `protube-favicon.svg`: thicker strokes (14%) so it stays readable at 32px.
- **Wordmark:** "ProTube" next to the mark, with "Pro" in the primary text colour and "Tube" in `--pt-primary-text`
  (the brand red, adjusted to reach 4.5:1 on each theme).
- **Clear space and size:** keep a free margin of one T-crossbar height around the logo. Minimum 24px for the
  icon and 120px wide for the full lockup.
- **Personality:** modern, clean and friendly; made by students, finished like a professional product.

## Colours

The theme is set with `data-bs-theme` on `<html>`. Users choose Light, Dark or System (the default, which follows
the operating system) from the navbar sun/moon button or Settings → Appearance; the choice is kept in
`localStorage` (`protube.theme`) by `src/theme/ThemeProvider.tsx`, and an inline script in `index.html` applies it
before the first paint. Dark is used when the system preference is unknown. The light variant redefines the
neutral tokens and darkens the accents used as text (`--pt-primary-text`, `--pt-danger-text`, `--pt-success`,
`--pt-info`), so they keep a 4.5:1 contrast.

| Token                 | Dark      | Light     | Use                              |
| --------------------- | --------- | --------- | -------------------------------- |
| `--pt-bg-body`        | `#212529` | `#F8F9FA` | Page background                  |
| `--pt-surface`        | `#2B3035` | `#FFFFFF` | Cards, panels, menus, inputs     |
| `--pt-border`         | `#343A40` | `#DEE2E6` | Dividers, card and input borders |
| `--pt-text-primary`   | `#F8F9FA` | `#212529` | Titles and body text             |
| `--pt-text-secondary` | `#ADB5BD` | `#5C636A` | Metadata, hints, timestamps      |

| Token                  | Value     | Use                                                                   |
| ---------------------- | --------- | --------------------------------------------------------------------- |
| `--pt-brand`           | `#E63946` | Brand red of the logo files only                                      |
| `--pt-primary`         | `#D62839` | Fill behind white text: primary buttons, badges, checked inputs       |
| `--pt-primary-hover`   | `#B71C1C` | Hover of primary elements                                             |
| `--pt-primary-pressed` | `#9B1717` | Pressed state                                                         |
| `--pt-primary-text`    | `#FF7A82` | Red as text: links, active states, outline buttons (light: `#C8102E`) |
| `--pt-success`         | `#2EC4B6` | Validation, success messages (light theme: `#0F766E`)                 |
| `--pt-warning`         | `#FFB703` | Warnings                                                              |
| `--pt-danger`          | `#DC3545` | Errors and destructive actions                                        |
| `--pt-danger-text`     | `#EA868F` | Danger as text: outline danger buttons (light theme: `#B02A37`)       |
| `--pt-info`            | `#4CC9F0` | Informative labels, hashtags (light theme: `#0B7A9E`)                 |

`--pt-primary-rgb` (`214, 40, 57`) is there for translucent tints, e.g. `rgba(var(--pt-primary-rgb), 0.12)`.

Any text on a background must reach a contrast ratio of at least 4.5:1 (WCAG 2.1 AA): white on `--pt-primary` is
4.97:1, but the brand red `#E63946` only reaches 4.17:1, so it is kept for the logo. Never use `--pt-primary` as a
text colour: use `--pt-primary-text`. The E2E suite checks it on every main page with axe
(`e2e/tests/accessibility.spec.ts`).

## Typography

Font stack: `Inter, system-ui, -apple-system, sans-serif`.

| Style            | Size / line height | Weight | Letter spacing                                     |
| ---------------- | ------------------ | ------ | -------------------------------------------------- |
| H1 / display     | 32 / 40px          | 700    | -0.02em                                            |
| H2               | 24 / 32px          | 600    | -0.01em                                            |
| H3               | 20 / 28px          | 600    | -0.005em                                           |
| H4 / subtitle    | 16 / 24px          | 500    | —                                                  |
| Body             | 14 / 20px          | 400    | —                                                  |
| Metadata / small | 12 / 16px          | 400    | — (colour `--pt-text-secondary`)                   |
| Video title      | 16 / 22px          | 500    | — (two lines max with `-webkit-line-clamp: 2`)     |
| Duration badge   | 11 / 14px          | 700    | 0.02em (white on a dark overlay, `--pt-radius-sm`) |

## Spacing, radius and grid

- Spacing in multiples of 4px: 4, 8, 16, 24, 32, 48 (Bootstrap spacers `1`–`5` cover most of them).
- Radius tokens: `--pt-radius-sm` 4px (badges, tags, compact inputs), `--pt-radius-md` 8px (thumbnails, cards,
  panels), `--pt-radius-lg` 12px (modals, player, menus), `--pt-radius-pill` (primary call-to-action buttons,
  filter pills).
- Bootstrap 12-column grid and breakpoints: `sm` 576px, `md` 768px, `lg` 992px, `xl` 1200px, `xxl` 1400px.

## Responsive (US-14)

Every page works from 320px (the narrowest common phone) up, with no horizontal scroll. Media queries use the
Bootstrap breakpoints above (`max-width: 575.98px` for phones, `991.98px` up to tablets); the only extra one is
`399.98px` for the navbar.

- **Navbar:** below 576px "Create" shows only its `+` icon (the text stays as its accessible name) and the gaps
  shrink; below 400px the brand shows only the logo, so every action fits at 320px. Buttons never wrap their text.
- **Tables:** on phones a data table becomes one block per row (first cell full width, then the values with their
  column name from `data-label`), instead of a table that scrolls sideways. The header stays for screen readers.
- **Section links and badges** wrap onto a new line instead of being cut or squeezed.
- Check new pages at 320, 360, 390 and 768px; `e2e/tests/responsive.spec.ts` fails if a page scrolls sideways.

## Components

- **Navbar:** sticky, height `--pt-navbar-height` (64px), background `--pt-bg-body` with a `--pt-border` bottom
  border. Logo on the left, search in the middle, user menu on the right (Log in button, or the user initial avatar
  with Settings and Logout).
- **Video card:** 16:9 thumbnail with `object-fit: cover` and `--pt-radius-md`, duration badge at the bottom-right,
  title (two lines max) and author below.
- **Video player page:** 16:9 player in a `--pt-radius-lg` frame, title, author, description and the comment list.
- **Buttons:**
  - Primary: background `--pt-primary`, white text, hover `--pt-primary-hover`, 2px light red focus ring.
  - Secondary: background `--pt-surface`, border `--pt-border`, text `--pt-text-primary`.
  - Ghost: transparent, border `--pt-border`, text `--pt-text-secondary`, hover background `--pt-surface`.
  - Disabled: 50% opacity and `cursor: not-allowed`.
- **Icons:** Bootstrap Icons style, rounded line icons, 20 or 24px.

## Out of scope for the MVP

The original style guide also draws an Upload button in the navbar, notifications, a Subscribe button, view
counts, verified badges and a `Ctrl+K` search shortcut. They are not in `REQUIREMENT.md`: design them only when a
user story asks for them.
