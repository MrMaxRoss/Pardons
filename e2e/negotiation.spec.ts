import { test, expect } from "./fixtures";
import { USERS, setNumberPickerValue } from "./helpers";

/**
 * Helper: Max creates an offer to Daphne, returns the transaction URL.
 */
async function maxCreatesOfferToDaphne(
  maxPage: import("@playwright/test").Page,
  amount: number = 1,
  description: string = "Test offer",
) {
  await maxPage.goto("/new");
  await maxPage.getByRole("button", { name: USERS.daphne.name }).click();
  if (amount > 1) {
    await setNumberPickerValue(maxPage, amount);
  }
  await maxPage.getByPlaceholder("e.g. For eating the last cookie").fill(description);
  await maxPage.getByRole("button", { name: "Send Offer" }).click();
  await maxPage.waitForURL(/\/transaction\/.+/);
  return maxPage.url();
}

test("accept: Daphne accepts Max's offer", async ({ maxPage, daphnePage }) => {
  const txUrl = await maxCreatesOfferToDaphne(maxPage);

  // Max should see "Waiting for Daphne to respond..."
  await expect(maxPage.locator("text=Waiting for Daphne to respond")).toBeVisible();

  // Daphne navigates to the same transaction
  await daphnePage.goto(txUrl);
  await expect(daphnePage.getByRole("button", { name: "Accept" })).toBeVisible();
  await expect(daphnePage.getByRole("button", { name: "Reject" })).toBeVisible();

  // Daphne accepts
  await daphnePage.getByRole("button", { name: "Accept" }).click();

  // Status should update to accepted
  await expect(daphnePage.locator("text=accepted").first()).toBeVisible();

  // Both users should see it in history tab
  await maxPage.goto("/");
  await maxPage.getByRole("button", { name: /History/ }).click();
  await expect(maxPage.locator("text=Test offer")).toBeVisible();

  await daphnePage.goto("/");
  await daphnePage.getByRole("button", { name: /History/ }).click();
  await expect(daphnePage.locator("text=Test offer")).toBeVisible();
});

test("reject: Max rejects Violet's request", async ({ maxPage, violetPage }) => {
  // Violet creates a request to Max
  await violetPage.goto("/new");
  await violetPage.getByRole("button", { name: "Request a Pardon" }).click();
  await violetPage.getByRole("button", { name: USERS.max.name }).click();
  await violetPage.getByPlaceholder("e.g. For eating the last cookie").fill("Please pardon me");
  await violetPage.getByRole("button", { name: "Send Request" }).click();
  await violetPage.waitForURL(/\/transaction\/.+/);
  const txUrl = violetPage.url();

  // Max navigates and rejects
  await maxPage.goto(txUrl);
  await maxPage.getByRole("button", { name: "Reject" }).click();

  await expect(maxPage.locator("text=rejected").first()).toBeVisible();
});

test("counter: Daphne counters Max's offer", async ({ maxPage, daphnePage }) => {
  const txUrl = await maxCreatesOfferToDaphne(maxPage, 5, "Counter test");

  // Daphne opens the transaction
  await daphnePage.goto(txUrl);

  // Click Counter button (should show "Counter (2 left)")
  await daphnePage.getByRole("button", { name: /Counter/ }).click();

  // Counter form appears - change amount (starts at currentAmount=5, go to 3)
  const counterForm = daphnePage.locator("text=Counter-offer").locator("..");
  await setNumberPickerValue(counterForm, 3, 5);

  // Send counter
  await daphnePage.getByRole("button", { name: "Send Counter" }).click();

  // Timeline should show counter event
  await expect(daphnePage.locator("text=Counter-offered")).toBeVisible();

  // Max should see updated amount when refreshing
  await maxPage.goto(txUrl);
  await expect(maxPage.locator("text=3").first()).toBeVisible();
  await expect(maxPage.locator("text=Counter-offered")).toBeVisible();
});

test("counter limit: Counter button disappears after 2 counters from one side", async ({
  maxPage,
  daphnePage,
}) => {
  const txUrl = await maxCreatesOfferToDaphne(maxPage, 5, "Counter limit test");

  // Counter 1: Daphne counters
  await daphnePage.goto(txUrl);
  await daphnePage.getByRole("button", { name: /Counter/ }).click();
  await daphnePage.getByRole("button", { name: "Send Counter" }).click();
  await expect(daphnePage.locator("text=Waiting for Max to respond")).toBeVisible();

  // Max counters back
  await maxPage.goto(txUrl);
  await maxPage.getByRole("button", { name: /Counter/ }).click();
  await maxPage.getByRole("button", { name: "Send Counter" }).click();
  await expect(maxPage.locator("text=Waiting for Daphne to respond")).toBeVisible();

  // Counter 2: Daphne counters again
  await daphnePage.goto(txUrl);
  await daphnePage.getByRole("button", { name: /Counter/ }).click();
  await daphnePage.getByRole("button", { name: "Send Counter" }).click();
  await expect(daphnePage.locator("text=Waiting for Max to respond")).toBeVisible();

  // Max should still see Counter since he's only used 1
  await maxPage.goto(txUrl);
  await expect(maxPage.getByRole("button", { name: /Counter/ })).toBeVisible();

  // Max counters again (his 2nd)
  await maxPage.getByRole("button", { name: /Counter/ }).click();
  await maxPage.getByRole("button", { name: "Send Counter" }).click();

  // Now Daphne has used 2 counters, so Counter button should NOT appear
  await daphnePage.goto(txUrl);
  await expect(daphnePage.getByRole("button", { name: "Accept" })).toBeVisible();
  await expect(daphnePage.getByRole("button", { name: "Reject" })).toBeVisible();
  await expect(daphnePage.getByRole("button", { name: /Counter/ })).not.toBeVisible();
});

test("turn enforcement: initiator sees waiting message after creating", async ({ maxPage }) => {
  await maxCreatesOfferToDaphne(maxPage);

  // Max should NOT see any action buttons
  await expect(maxPage.getByRole("button", { name: "Accept" })).not.toBeVisible();
  await expect(maxPage.getByRole("button", { name: "Reject" })).not.toBeVisible();
  await expect(maxPage.locator("text=Waiting for Daphne to respond")).toBeVisible();
});

test("full negotiation: offer → counter → counter → accept", async ({ maxPage, daphnePage }) => {
  // Max offers 5 to Daphne
  const txUrl = await maxCreatesOfferToDaphne(maxPage, 5, "Full negotiation");

  // Daphne counters with 3
  await daphnePage.goto(txUrl);
  await daphnePage.getByRole("button", { name: /Counter/ }).click();
  const counterForm1 = daphnePage.locator("text=Counter-offer").locator("..");
  await setNumberPickerValue(counterForm1, 3, 5);
  await daphnePage.getByRole("button", { name: "Send Counter" }).click();
  await expect(daphnePage.locator("text=Waiting for Max to respond")).toBeVisible();

  // Max counters with 4
  await maxPage.goto(txUrl);
  await maxPage.getByRole("button", { name: /Counter/ }).click();
  const counterForm2 = maxPage.locator("text=Counter-offer").locator("..");
  await setNumberPickerValue(counterForm2, 4, 3);
  await maxPage.getByRole("button", { name: "Send Counter" }).click();
  await expect(maxPage.locator("text=Waiting for Daphne to respond")).toBeVisible();

  // Daphne accepts
  await daphnePage.goto(txUrl);
  await daphnePage.getByRole("button", { name: "Accept" }).click();
  await expect(daphnePage.locator("text=accepted").first()).toBeVisible();

  // Final amount should be 4
  await expect(daphnePage.locator("text=4").first()).toBeVisible();
});
