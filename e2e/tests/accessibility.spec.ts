import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Locator, type Page } from '@playwright/test';
import { loginAsNewUser, registerNewUser, SEED_VIDEOS, uploadVideoAs } from './support';

// WCAG 2.1 level A and AA, the level the design system asks for (docs/DESIGN_SYSTEM.md)
const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];
const THEMES = ['light', 'dark'] as const;

/**
 * Runs axe on the current page once `ready` is visible. A failure lists every violation with its rule, the elements
 * and why they fail (e.g. the contrast ratio found). Soft, so one run reports the problems of every page.
 */
async function expectNoViolations(page: Page, ready: Locator): Promise<void> {
  await expect(ready).toBeVisible();
  const { violations } = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();
  const report = violations.map((violation) => {
    const nodes = violation.nodes.map((node) => {
      const why = node.failureSummary?.replace(/\s+/g, ' ') ?? '';
      return `    ${node.target.join(' ')}: ${why}`;
    });
    return [`${violation.id} (${violation.impact}): ${violation.help}`, ...nodes].join('\n');
  });
  expect.soft(report, `accessibility violations on ${page.url()}`).toEqual([]);
}

for (const theme of THEMES) {
  test.describe(`Accessibility (${theme} theme)`, () => {
    // No transitions or hover previews running while axe reads the colours
    test.use({ reducedMotion: 'reduce' });

    test.beforeEach(async ({ page }) => {
      // Same key as src/theme/themePreference.ts in the frontend
      await page.addInitScript((value) => localStorage.setItem('protube.theme', value), theme);
    });

    test('the pages a visitor sees have no WCAG 2.1 AA violations', async ({ page }) => {
      await page.goto('/');
      await expectNoViolations(page, page.getByRole('link', { name: SEED_VIDEOS.mountain }).first());

      await page.goto(`/?q=${encodeURIComponent(SEED_VIDEOS.city)}`);
      await expectNoViolations(page, page.getByRole('list', { name: `Results for "${SEED_VIDEOS.city}"` }));

      await page.getByRole('link', { name: SEED_VIDEOS.city }).first().click();
      await expectNoViolations(page, page.getByRole('heading', { level: 1, name: SEED_VIDEOS.city }));

      await page.goto('/login');
      await expectNoViolations(page, page.getByRole('heading', { name: 'Log in' }).first());

      await page.getByRole('button', { name: 'Register', exact: true }).click();
      await expectNoViolations(page, page.getByRole('heading', { name: 'Create account' }));
    });

    test('the pages of a logged-in user have no WCAG 2.1 AA violations', async ({ page, request }) => {
      const user = await loginAsNewUser(page, request);
      const author = await registerNewUser(request, 'a11y');
      const title = `A11y video ${Date.now()}`;
      const id = await uploadVideoAs(request, author, title);

      await page.goto(`/videos/${id}`);
      await expectNoViolations(page, page.getByRole('heading', { level: 1, name: title }));

      await page.goto('/upload');
      await expectNoViolations(page, page.getByRole('heading', { level: 1 }));

      await page.goto('/settings');
      await expectNoViolations(page, page.getByRole('heading', { level: 1 }));

      await page.goto('/favourites');
      await expectNoViolations(page, page.getByRole('heading', { level: 1 }));

      await page.goto('/subscriptions');
      await expectNoViolations(page, page.getByRole('heading', { level: 1 }));

      await page.goto('/analytics');
      await expectNoViolations(page, page.getByRole('heading', { level: 1 }));

      // US-03: public profile of another user, then your own with the edit form open
      await page.goto(`/profile/${author.username}`);
      await expectNoViolations(page, page.getByRole('heading', { level: 1, name: author.username }));

      await page.goto(`/profile/${user.username}`);
      await page.getByRole('button', { name: 'Edit profile' }).click();
      await expectNoViolations(page, page.getByRole('heading', { name: 'Edit your profile' }));
    });
  });
}
