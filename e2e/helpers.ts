import { Page } from "@playwright/test";

export const USERS = {
  max: { email: "max.ross@gmail.com", name: "Max" },
  daphne: { email: "daphne.ross@gmail.com", name: "Daphne" },
  violet: { email: "violet.ross@gmail.com", name: "Violet" },
} as const;

const FIRESTORE_PROJECT = "pardons-local";
const FIRESTORE_HOST = "http://localhost:8181";

/**
 * Clear all Firestore data via the emulator REST API.
 */
export async function clearFirestore() {
  const url = `${FIRESTORE_HOST}/emulator/v1/projects/${FIRESTORE_PROJECT}/databases/(default)/documents`;
  const res = await fetch(url, { method: "DELETE" });
  if (!res.ok) {
    throw new Error(`Failed to clear Firestore: ${res.status} ${res.statusText}`);
  }
}

const BACKEND_URL = "http://localhost:8080";

/**
 * Register all dev users in Firestore via the backend API.
 * This ensures the FamilyMemberPicker shows all users.
 */
export async function registerAllUsers() {
  for (const user of Object.values(USERS)) {
    await fetch(`${BACKEND_URL}/api/users/me`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer dev:${user.email}`,
      },
      body: JSON.stringify({}),
    });
  }
}

/**
 * Log in as a dev user by navigating to /login and clicking the user button.
 */
export async function loginAs(page: Page, user: { email: string; name: string }) {
  await page.goto("/login");
  await page.getByRole("button", { name: user.name }).click();
  await page.waitForURL("/");
}

/**
 * Click the + button on a NumberPicker the specified number of times.
 * Finds the NumberPicker relative to the provided parent locator, or the page root.
 */
export async function setNumberPickerValue(
  parent: Page | ReturnType<Page["locator"]>,
  targetValue: number,
  currentValue: number = 1,
) {
  const clicks = targetValue - currentValue;
  const button = clicks > 0
    ? parent.locator("button", { hasText: "+" })
    : parent.locator("button", { hasText: "-" });
  for (let i = 0; i < Math.abs(clicks); i++) {
    await button.click();
  }
}
