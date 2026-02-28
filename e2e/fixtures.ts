import { test as base, Page, Browser } from "@playwright/test";
import { USERS, loginAs, clearFirestore, registerAllUsers } from "./helpers";

type UserFixtures = {
  cleanDb: void;
  maxPage: Page;
  daphnePage: Page;
  violetPage: Page;
};

async function createAuthenticatedPage(
  browser: Browser,
  user: { email: string; name: string },
): Promise<Page> {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginAs(page, user);
  return page;
}

export const test = base.extend<UserFixtures>({
  cleanDb: [async ({}, use) => {
    await clearFirestore();
    await registerAllUsers();
    await use();
  }, { auto: false }],
  maxPage: async ({ browser, cleanDb: _ }, use) => {
    const page = await createAuthenticatedPage(browser, USERS.max);
    await use(page);
    await page.context().close();
  },
  daphnePage: async ({ browser, cleanDb: _ }, use) => {
    const page = await createAuthenticatedPage(browser, USERS.daphne);
    await use(page);
    await page.context().close();
  },
  violetPage: async ({ browser, cleanDb: _ }, use) => {
    const page = await createAuthenticatedPage(browser, USERS.violet);
    await use(page);
    await page.context().close();
  },
});

export { expect } from "@playwright/test";
