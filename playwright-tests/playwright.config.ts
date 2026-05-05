import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  retries: 2,
  workers: 1,
  reporter: [['list'], ['json', { outputFile: '/app/results.json' }]],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5126',
    screenshot: 'only-on-failure',
    trace: 'on',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
