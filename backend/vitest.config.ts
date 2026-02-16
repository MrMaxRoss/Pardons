import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    globals: true,
    environment: "node",
    root: "src",
    env: {
      NODE_ENV: "test",
      DEV_AUTH: "true",
    },
  },
});
