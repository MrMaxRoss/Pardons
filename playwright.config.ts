import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  workers: 1,
  reporter: "list",
  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "chromium",
      use: { browserName: "chromium" },
    },
  ],
  webServer: [
    {
      command: "FIRESTORE_EMULATOR_HOST=localhost:8181 DEV_AUTH=true npm run dev --workspace=backend",
      port: 8080,
      reuseExistingServer: !process.env.CI,
    },
    {
      command: "npm run dev --workspace=frontend",
      port: 5173,
      reuseExistingServer: !process.env.CI,
    },
  ],
});
