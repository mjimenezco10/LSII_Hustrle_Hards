import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';
import { loginAsNewUser, openVideoFromSearch, SEED_VIDEOS, userMenu } from './support';

// Any small image works as an avatar: the thumbnail of a fixture video (WebP)
const AVATAR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
  'fixtures',
  'store',
  'e2e-mountain-sunrise.webp'
);

test.describe('User profile (US-03)', () => {
  test('a user edits their profile and avatar, and others see it without edit controls', async ({
    page,
    request,
    browser,
  }) => {
    const user = await loginAsNewUser(page, request);
    const description = `I film timelapses ${Date.now()}`;

    await page.goto('/');
    await userMenu(page, user.username).click();
    await page.getByRole('button', { name: 'Your profile' }).click();
    await expect(page).toHaveURL(new RegExp(`/profile/${user.username}$`));
    await expect(page.getByRole('heading', { level: 1, name: user.username })).toBeVisible();
    await expect(page.getByText('No description yet.')).toBeVisible();

    await page.getByRole('button', { name: 'Edit profile' }).click();
    await page.getByLabel('Description').fill(description);
    await page.getByLabel('Location').fill('Mataró');
    await page.getByLabel('Website').fill('https://example.com/me');
    await page.getByRole('button', { name: 'Save profile' }).click();
    await expect(page.getByRole('status')).toHaveText('Profile saved.');

    await page.getByLabel('Avatar').setInputFiles(AVATAR);
    await expect(page.getByText('Avatar updated.')).toBeVisible();
    const avatar = page.getByRole('img', { name: `Avatar of ${user.username}` });
    await expect(avatar).toBeVisible();
    // The image really loads from the media server
    await expect.poll(() => avatar.evaluate((img: HTMLImageElement) => img.naturalWidth)).toBeGreaterThan(0);

    // Saved: still there after a reload
    await page.reload();
    await expect(page.getByText(description)).toBeVisible();
    await expect(page.getByText('Mataró')).toBeVisible();
    await expect(page.getByRole('link', { name: 'https://example.com/me' })).toBeVisible();

    // Another visitor sees the public profile, without edit controls
    const visitor = await browser.newPage();
    await visitor.goto(page.url());
    await expect(visitor.getByText(description)).toBeVisible();
    await expect(visitor.getByRole('img', { name: `Avatar of ${user.username}` })).toBeVisible();
    await expect(visitor.getByRole('button', { name: 'Edit profile' })).toBeHidden();
    await visitor.close();
  });

  test('an invalid website is rejected and nothing is saved', async ({ page, request }) => {
    const user = await loginAsNewUser(page, request);

    await page.goto(`/profile/${user.username}`);
    await page.getByRole('button', { name: 'Edit profile' }).click();
    await page.getByLabel('Website').fill('example.com');
    await page.getByRole('button', { name: 'Save profile' }).click();
    await expect(page.getByText('Website must start with http:// or https://')).toBeVisible();

    await page.reload();
    await expect(page.getByText('No description yet.')).toBeVisible();
    await expect(page.getByRole('link', { name: 'example.com' })).toBeHidden();
  });

  test('the author of a video links to its channel profile with its videos', async ({ page }) => {
    await openVideoFromSearch(page, SEED_VIDEOS.mountain);

    // Author of the fixture video (the "user" of its .json in fixtures/store)
    const name = 'e2e-seed';
    await page.getByRole('link', { name, exact: true }).click();

    await expect(page.getByRole('heading', { level: 1, name })).toBeVisible();
    await expect(page.getByText('Channel of imported videos')).toBeVisible();
    await expect(
      page.getByRole('list', { name: `Videos of ${name}` }).getByRole('link', { name: SEED_VIDEOS.mountain })
    ).toBeVisible();
  });

  test('an unknown profile shows that it does not exist', async ({ page }) => {
    await page.goto('/profile/nobody_at_all_e2e');
    await expect(page.getByRole('heading', { name: 'Profile not found' })).toBeVisible();
  });
});
