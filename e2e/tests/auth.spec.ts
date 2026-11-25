import { expect, test, type Page } from "@playwright/test";
import { BACKEND_URL } from "../env";
import { newUser, registerNewUser, userMenu } from "./support";

/** An unsigned JWT for `sub` that expires `seconds` from now (the frontend only reads its "exp" before renewing). */
function fakeToken(sub: string, seconds: number): string {
  const payload = { sub, exp: Math.floor(Date.now() / 1000) + seconds };
  return `e30.${Buffer.from(JSON.stringify(payload)).toString("base64url")}.signature`;
}

/** Opens the app with this session stored (same key and format as src/auth/session.ts in the frontend). */
async function storeSession(page: Page, session: object): Promise<void> {
  await page.addInitScript(
    (value) => localStorage.setItem("protube.session", value),
    JSON.stringify(session),
  );
}

test.describe("Authentication", () => {
  test("a visitor registers, logs out and logs in again", async ({ page }) => {
    const user = newUser();

    await page.goto("/");
    await page.getByRole("button", { name: "Log in", exact: true }).click();
    await expect(page).toHaveURL(/\/login$/);

    // Register
    await page.getByRole("button", { name: "Register", exact: true }).click();
    await expect(
      page.getByRole("heading", { name: "Create account" }),
    ).toBeVisible();
    await page.getByLabel("Username").fill(user.username);
    await page.getByLabel("Email").fill(user.email);
    await page.getByLabel("Password", { exact: true }).fill(user.password);
    await page.getByRole("button", { name: "Register", exact: true }).click();

    await expect(page).toHaveURL(/\/$/);
    await expect(userMenu(page, user.username)).toBeVisible();

    // The session survives a reload
    await page.reload();
    await expect(userMenu(page, user.username)).toBeVisible();

    // Logout
    await userMenu(page, user.username).click();
    await page.getByRole("button", { name: "Logout" }).click();
    await expect(
      page.getByRole("button", { name: "Log in", exact: true }),
    ).toBeVisible();
    await expect(userMenu(page, user.username)).toBeHidden();

    // Login
    await page.getByRole("button", { name: "Log in", exact: true }).click();
    await page.getByLabel("Username").fill(user.username);
    await page.getByLabel("Password", { exact: true }).fill(user.password);
    await page.getByRole("button", { name: "Login", exact: true }).click();

    await expect(page).toHaveURL(/\/$/);
    await expect(userMenu(page, user.username)).toBeVisible();
  });

  test("a wrong password is rejected", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Username").fill("nobody_e2e");
    await page.getByLabel("Password", { exact: true }).fill("wrong-password");
    await page.getByRole("button", { name: "Login", exact: true }).click();

    await expect(page.getByRole("alert")).toHaveText(
      "Invalid username or password",
    );
    await expect(page).toHaveURL(/\/login$/);
  });

  test("registering an existing username shows the error of the backend", async ({
    page,
    request,
  }) => {
    const user = await registerNewUser(request);

    await page.goto("/login");
    await page.getByRole("button", { name: "Register", exact: true }).click();
    await page.getByLabel("Username").fill(user.username);
    await page.getByLabel("Email").fill(`other-${user.email}`);
    await page.getByLabel("Password", { exact: true }).fill(user.password);
    await page.getByRole("button", { name: "Register", exact: true }).click();

    await expect(page.getByRole("alert")).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test("registering an email that already has an account is rejected", async ({
    page,
    request,
  }) => {
    const user = await registerNewUser(request);

    await page.goto("/login");
    await page.getByRole("button", { name: "Register", exact: true }).click();
    await page.getByLabel("Username").fill(`${user.username}_2`);
    // Same email with other capital letters: still the same account
    await page.getByLabel("Email").fill(user.email.toUpperCase());
    await page.getByLabel("Password", { exact: true }).fill(user.password);
    await page.getByRole("button", { name: "Register", exact: true }).click();

    await expect(page.getByRole("alert")).toHaveText(
      "Email is already registered",
    );
    await expect(page).toHaveURL(/\/login$/);
  });

  test("an anonymous visitor who wants to upload is sent to the login page", async ({
    page,
  }) => {
    await page.goto("/");
    await page.getByRole("button", { name: "Create" }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole("heading", { name: "Log in" })).toBeVisible();
  });

  test("a session whose access token expired is renewed with the refresh token", async ({
    page,
    request,
  }) => {
    const user = newUser();
    const created = await request.post(`${BACKEND_URL}/api/auth/register`, {
      data: user,
    });
    expect(created.status()).toBe(201);
    const session = await created.json();
    // Same session, but its access token expired a minute ago
    await storeSession(page, {
      ...session,
      token: fakeToken(user.username, -60),
    });

    const renewal = page.waitForResponse((response) =>
      response.url().endsWith("/api/auth/refresh"),
    );
    await page.goto("/");

    expect((await renewal).status()).toBe(200);
    await expect(userMenu(page, user.username)).toBeVisible();
    // The stored session now has a new access token that the backend accepts
    const stored = await page.evaluate(() =>
      JSON.parse(localStorage.getItem("protube.session") ?? "null"),
    );
    const me = await request.get(`${BACKEND_URL}/api/auth/me`, {
      headers: { Authorization: `Bearer ${stored.token}` },
    });
    expect(me.status()).toBe(200);
  });

  test("a session whose refresh token is rejected ends", async ({ page }) => {
    await storeSession(page, {
      token: fakeToken("ghost_e2e", -60),
      refreshToken: fakeToken("ghost_e2e", 3600),
      user: { id: 0, username: "ghost_e2e", email: "ghost@e2e.test" },
    });

    await page.goto("/");

    await expect(
      page.getByRole("button", { name: "Log in", exact: true }),
    ).toBeVisible();
    await expect(userMenu(page, "ghost_e2e")).toBeHidden();
  });
});
