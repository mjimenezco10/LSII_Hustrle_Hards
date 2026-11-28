import { expect, test, type Locator, type Page } from '@playwright/test';
import { expectVideoToPlay, findInVideoGrid, loginAsNewUser, SEED_VIDEOS, uploadVideoAs, userMenu } from './support';

// HH-91: common phone and tablet sizes. 320px is the narrowest phone still in use (iPhone SE 1st gen, small Android)
const PHONE = {
  viewport: { width: 360, height: 740 },
  isMobile: true,
  hasTouch: true,
};
const SMALL_PHONE = {
  viewport: { width: 320, height: 640 },
  isMobile: true,
  hasTouch: true,
};
const TABLET = {
  viewport: { width: 768, height: 1024 },
  isMobile: true,
  hasTouch: true,
};

/**
 * The page fits the screen: nothing makes it scroll sideways.
 */
async function expectNoHorizontalScroll(page: Page): Promise<void> {
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth), {
      message: 'the page should not scroll horizontally',
    })
    .toBe(0);
}

/**
 * Nothing inside the element scrolls sideways either (e.g. a wide table in a scrolling box). Clipped content
 * (overflow: hidden, such as a title cut with an ellipsis) does not count: it cannot be scrolled.
 */
async function expectNoSidewaysScrollInside(locator: Locator): Promise<void> {
  await expect
    .poll(
      () =>
        locator.evaluate(
          (root) =>
            [root, ...root.querySelectorAll('*')].filter(
              (el) => ['auto', 'scroll'].includes(getComputedStyle(el).overflowX) && el.scrollWidth > el.clientWidth + 1
            ).length
        ),
      { message: 'no element inside should scroll horizontally' }
    )
    .toBe(0);
}

/**
 * Once scrolled to (vertically), the element is entirely on screen, not cut by the left or right edge.
 */
async function expectFullyOnScreen(locator: Locator): Promise<void> {
  await expect(locator).toBeVisible();
  await locator.scrollIntoViewIfNeeded();
  await expect(locator).toBeInViewport({ ratio: 1 });
}

test.describe('Responsive layout on a phone', () => {
  test.use(PHONE);

  test('the video grid fits the screen and a video plays full width', async ({ page }) => {
    await page.goto('/');
    const video = await findInVideoGrid(page, SEED_VIDEOS.mountain);
    await expectNoHorizontalScroll(page);
    for (const action of ['Create', 'Settings', 'Log in']) {
      await expectFullyOnScreen(page.getByRole('button', { name: action }));
    }

    await video.click();
    const player = page.getByLabel(`Video player: ${SEED_VIDEOS.mountain}`);
    await expectVideoToPlay(player);
    await expectFullyOnScreen(player);
    await expectNoHorizontalScroll(page);
  });

  test('the user menu opens inside the screen and its items work', async ({ page, request }) => {
    const user = await loginAsNewUser(page, request);
    await page.goto('/');

    await userMenu(page, user.username).click();
    for (const item of ['Your profile', 'Favourites', 'Subscriptions', 'Analytics', 'Logout']) {
      await expectFullyOnScreen(page.getByRole('button', { name: item }));
    }
    await page.getByRole('button', { name: 'Favourites' }).click();

    await expect(page.getByRole('heading', { level: 1, name: 'Favourites' })).toBeVisible();
    await expectNoHorizontalScroll(page);
  });

  test('a form can be filled and sent on a phone', async ({ page, request }) => {
    const owner = await loginAsNewUser(page, request);
    const videoId = await uploadVideoAs(request, owner, `E2E phone ${Date.now()}`);
    const newTitle = `E2E edited on a phone ${Date.now()}`;

    await page.goto(`/videos/${videoId}/edit`);
    await expect(page.getByRole('heading', { level: 1, name: 'Edit video' })).toBeVisible();
    await expectNoHorizontalScroll(page);
    await page.getByLabel('Title (required)').fill(newTitle);
    const save = page.getByRole('button', { name: 'Save' });
    await expectFullyOnScreen(save);
    await save.click();

    await expect(page.getByRole('heading', { level: 1, name: newTitle })).toBeVisible();
  });

  test('the analytics of each video are listed as blocks instead of a wide table', async ({ page, request }) => {
    const owner = await loginAsNewUser(page, request);
    const title = `E2E phone analytics ${Date.now()}`;
    await uploadVideoAs(request, owner, title);

    await page.goto('/analytics');
    const videos = page.getByRole('region', { name: 'Your videos' });
    await expect(videos.getByRole('link', { name: title })).toBeVisible();
    // The last column (comment likes) used to be off screen, behind a sideways scroll
    await expectNoSidewaysScrollInside(videos);
    await expectFullyOnScreen(videos.getByRole('cell', { name: 'Comment likes 0' }));
    await expectNoHorizontalScroll(page);
  });
});

test.describe('Responsive layout on the smallest phones', () => {
  test.use(SMALL_PHONE);

  test('every navbar action stays reachable at 320px, logged out and logged in', async ({ page, request }) => {
    await page.goto('/');
    await expect(page.getByRole('list', { name: 'Videos' })).toBeVisible();
    // Only the logo is left: its button keeps the name
    await expectFullyOnScreen(page.getByRole('button', { name: 'ProTube' }));
    for (const action of ['Create', 'Settings', 'Log in']) {
      await expectFullyOnScreen(page.getByRole('button', { name: action }));
    }
    await expectNoHorizontalScroll(page);

    const user = await loginAsNewUser(page, request);
    await page.goto('/');
    await expectFullyOnScreen(userMenu(page, user.username));
    await expectNoHorizontalScroll(page);
  });
});

test.describe('Responsive layout on a tablet', () => {
  test.use(TABLET);

  test('no page scrolls sideways', async ({ page, request }) => {
    const user = await loginAsNewUser(page, request);
    const videoId = await uploadVideoAs(request, user, `E2E tablet ${Date.now()}`);

    await page.goto('/');
    await expect(page.getByRole('list', { name: 'Videos' })).toBeVisible();
    await expectNoHorizontalScroll(page);

    const pages = [
      `/videos/${videoId}`,
      `/videos/${videoId}/edit`,
      '/upload',
      '/settings',
      '/analytics',
      '/favourites',
      '/subscriptions',
      `/profile/${user.username}`,
    ];
    for (const path of pages) {
      await page.goto(path);
      await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible();
      await expectNoHorizontalScroll(page);
    }
  });
});
