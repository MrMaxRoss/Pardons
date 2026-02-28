import { test, expect } from "./fixtures";
import { USERS, setNumberPickerValue } from "./helpers";

test("create an offer with correct data", async ({ maxPage }) => {
  const page = maxPage;
  await page.goto("/new");

  // "Offer a Pardon" is selected by default
  await expect(page.getByRole("button", { name: "Offer a Pardon" })).toBeVisible();

  // Pick target: Daphne
  await page.getByRole("button", { name: USERS.daphne.name }).click();

  // Set amount to 3 (default is 1, click + twice)
  await setNumberPickerValue(page, 3);

  // Enter description
  await page.getByPlaceholder("e.g. For eating the last cookie").fill("For being awesome");

  // Submit
  await page.getByRole("button", { name: "Send Offer" }).click();

  // Should redirect to transaction detail
  await page.waitForURL(/\/transaction\/.+/);

  // Verify transaction data
  await expect(page.locator("text=offer").first()).toBeVisible();
  await expect(page.locator("text=For being awesome")).toBeVisible();
  await expect(page.getByText("3", { exact: true })).toBeVisible();
  await expect(page.getByText("To: Daphne")).toBeVisible();
  await expect(page.locator("text=pending")).toBeVisible();
});

test("create a request", async ({ maxPage }) => {
  const page = maxPage;
  await page.goto("/new");

  // Switch to "Request a Pardon"
  await page.getByRole("button", { name: "Request a Pardon" }).click();

  // Pick target: Violet
  await page.getByRole("button", { name: USERS.violet.name }).click();

  // Enter description
  await page.getByPlaceholder("e.g. For eating the last cookie").fill("For borrowing my book");

  // Submit
  await page.getByRole("button", { name: "Send Request" }).click();

  // Should redirect to transaction detail
  await page.waitForURL(/\/transaction\/.+/);

  await expect(page.locator("text=request").first()).toBeVisible();
  await expect(page.locator("text=For borrowing my book")).toBeVisible();
});

test("validation error when fields are missing", async ({ maxPage }) => {
  const page = maxPage;
  await page.goto("/new");

  // Submit without filling in anything
  await page.getByRole("button", { name: "Send Offer" }).click();

  await expect(page.locator("text=Please fill in all fields")).toBeVisible();
});

test("new pardon appears in home page pending tab", async ({ maxPage }) => {
  const page = maxPage;

  // Create a pardon
  await page.goto("/new");
  await page.getByRole("button", { name: USERS.daphne.name }).click();
  await page.getByPlaceholder("e.g. For eating the last cookie").fill("Test pardon");
  await page.getByRole("button", { name: "Send Offer" }).click();
  await page.waitForURL(/\/transaction\/.+/);

  // Go home
  await page.goto("/");

  // Should see the transaction in pending tab
  await expect(page.locator("text=Test pardon")).toBeVisible();
});
