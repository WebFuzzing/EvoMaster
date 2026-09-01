const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './generated',
  testMatch: /.*[tT]est\.js/,
});
