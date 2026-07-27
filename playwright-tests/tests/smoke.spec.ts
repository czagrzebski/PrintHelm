import { test, expect } from '@playwright/test'

test('login and reach dashboard', async ({ page }) => {
  const username = process.env.PLAYWRIGHT_USERNAME
  const password = process.env.PLAYWRIGHT_PASSWORD

  if (!username || !password) {
    throw new Error('PLAYWRIGHT_USERNAME and PLAYWRIGHT_PASSWORD must be set')
  }

  // When running inside Docker the frontend JS still calls the external API origin.
  // Intercept those requests in the Playwright process (Node.js, not the browser)
  // and forward them directly to the internal backend service on the Docker network.
  const externalApiOrigin = process.env.PLAYWRIGHT_EXTERNAL_API_ORIGIN
  const internalApiUrl = process.env.PLAYWRIGHT_INTERNAL_API_URL

  if (externalApiOrigin && internalApiUrl) {
    await page.route(`${externalApiOrigin}/**`, async (route) => {
      try {
        const rewritten = route.request().url().replace(externalApiOrigin, internalApiUrl)
        const response = await route.fetch({ url: rewritten })
        await route.fulfill({ response })
      } catch {
        // Page/context may already be tearing down (e.g. background polling
        // outliving the test) — nothing to fulfill in that case.
      }
    })
  }

  await page.goto('/login')

  await page.fill('#username', username)
  await page.fill('input[autocomplete="current-password"]', password)
  await page.click('button[type="submit"]')

  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 })
})
