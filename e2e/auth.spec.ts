import { test, expect } from "@playwright/test";
import { USERS, clearFirestore } from "./helpers";

test.beforeEach(async () => {
  await clearFirestore();
});

test("login page shows 3 dev user buttons", async ({ page }) => {
  await page.goto("/login");
  for (const user of Object.values(USERS)) {
    await expect(page.getByRole("button", { name: user.name })).toBeVisible();
  }
});

test("clicking a dev user button logs in and redirects to home", async ({ page }) => {
  await page.goto("/login");
  await page.getByRole("button", { name: USERS.max.name }).click();
  await page.waitForURL("/");
  // Nav should show user name
  await expect(page.locator("nav")).toContainText(USERS.max.name);
});

test("sign out redirects to login and clears auth", async ({ page }) => {
  // Log in first
  await page.goto("/login");
  await page.getByRole("button", { name: USERS.max.name }).click();
  await page.waitForURL("/");

  // Sign out
  await page.getByRole("button", { name: "Sign out" }).click();
  await page.waitForURL("/login");

  // Should see login page again
  await expect(page.getByRole("button", { name: USERS.max.name })).toBeVisible();

  // localStorage should be cleared
  const token = await page.evaluate(() => localStorage.getItem("token"));
  expect(token).toBeNull();
});

test("visiting protected route without auth redirects to login", async ({ page }) => {
  await page.goto("/");
  await page.waitForURL("/login");
});
