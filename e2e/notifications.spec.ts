import { test, expect } from "./fixtures";
import { USERS } from "./helpers";

test("notification badge appears after receiving an offer", async ({ maxPage, daphnePage }) => {
  // Max creates an offer to Daphne
  await maxPage.goto("/new");
  await maxPage.getByRole("button", { name: USERS.daphne.name }).click();
  await maxPage.getByPlaceholder("e.g. For eating the last cookie").fill("Notification test");
  await maxPage.getByRole("button", { name: "Send Offer" }).click();
  await maxPage.waitForURL(/\/transaction\/.+/);

  // Daphne reloads her home page — badge should show
  await daphnePage.goto("/");
  await expect(daphnePage.getByTestId("notification-count")).toBeVisible();
  await expect(daphnePage.getByTestId("notification-count")).toHaveText("1");
});

test("clicking notification navigates to transaction", async ({ maxPage, daphnePage }) => {
  // Max creates an offer to Daphne
  await maxPage.goto("/new");
  await maxPage.getByRole("button", { name: USERS.daphne.name }).click();
  await maxPage.getByPlaceholder("e.g. For eating the last cookie").fill("Click notification test");
  await maxPage.getByRole("button", { name: "Send Offer" }).click();
  await maxPage.waitForURL(/\/transaction\/.+/);

  // Daphne goes home and clicks the notification bell
  await daphnePage.goto("/");
  await daphnePage.getByLabel("Notifications").click();

  // Click the notification in the dropdown (scope to dropdown panel)
  await daphnePage.locator(".absolute.right-0 >> text=Click notification test").click();

  // Should navigate to the transaction
  await daphnePage.waitForURL(/\/transaction\/.+/);
  await expect(daphnePage.locator("text=Click notification test")).toBeVisible();
});

test("viewing a transaction clears its notifications", async ({ maxPage, daphnePage }) => {
  // Max creates an offer to Daphne
  await maxPage.goto("/new");
  await maxPage.getByRole("button", { name: USERS.daphne.name }).click();
  await maxPage.getByPlaceholder("e.g. For eating the last cookie").fill("Clear notification test");
  await maxPage.getByRole("button", { name: "Send Offer" }).click();
  await maxPage.waitForURL(/\/transaction\/.+/);
  const txUrl = maxPage.url();

  // Daphne should have a notification
  await daphnePage.goto("/");
  await expect(daphnePage.getByTestId("notification-count")).toBeVisible();

  // Daphne views the transaction directly
  await daphnePage.goto(txUrl);
  await expect(daphnePage.locator("text=Clear notification test")).toBeVisible();

  // Wait for notification clearing to complete, then go home
  await daphnePage.waitForTimeout(500);
  await daphnePage.goto("/");

  // Badge should be gone
  await expect(daphnePage.getByTestId("notification-count")).not.toBeVisible();
});
