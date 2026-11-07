import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";

/**
 * Vite configuration for the playground web app.
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: [{
      // Monaco ships a vendored sanitizer; replace that copy as well as its npm dependency.
      find: "./dompurify/dompurify.js",
      replacement: fileURLToPath(new URL("./node_modules/dompurify/dist/purify.es.mjs", import.meta.url))
    }]
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setupTests.ts"
  }
});
