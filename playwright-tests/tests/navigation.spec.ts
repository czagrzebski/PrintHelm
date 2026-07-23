import { test, expect, type Page } from '@playwright/test'

async function login(page: Page) {
  const username = process.env.PLAYWRIGHT_USERNAME
  const password = process.env.PLAYWRIGHT_PASSWORD

  if (!username || !password) {
    throw new Error('PLAYWRIGHT_USERNAME and PLAYWRIGHT_PASSWORD must be set')
  }

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
}

test.describe('authenticated navigation', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('sidebar renders all nav links', async ({ page }) => {
    const nav = page.getByRole('navigation')
    await expect(nav.getByRole('link', { name: /Dashboard/ })).toHaveAttribute('href', '/dashboard')
    await expect(nav.getByRole('link', { name: /Job Orders/ })).toHaveAttribute('href', '/job-orders')
    await expect(nav.getByRole('link', { name: /Print Queue/ })).toHaveAttribute('href', '/queue')
    await expect(nav.getByRole('link', { name: /Filament/ })).toHaveAttribute('href', '/filament')
    await expect(nav.getByRole('link', { name: /Analytics/ })).toHaveAttribute('href', '/analytics')
    await expect(nav.getByRole('link', { name: /Audit Log/ })).toHaveAttribute('href', '/audit')
    await expect(nav.getByRole('link', { name: /Settings/ })).toHaveAttribute('href', '/settings')
  })

  test('dashboard shows stat cards and printer table', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()

    await expect(page.getByText('Total Printers').first()).toBeVisible()
    await expect(page.getByText('Printing').first()).toBeVisible()
    await expect(page.getByText('Idle').first()).toBeVisible()
    await expect(page.getByText('Offline').first()).toBeVisible()

    const table = page.getByRole('table')
    await expect(table.getByRole('columnheader', { name: 'Name' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Status' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Progress' })).toBeVisible()
    await expect(table.getByRole('row').nth(1)).toBeVisible()
  })

  test('job orders page is accessible with expected columns', async ({ page }) => {
    await page.goto('/job-orders')
    const table = page.getByRole('table')
    await expect(table).toBeVisible()

    await expect(page.getByRole('heading', { name: 'Job Orders' })).toBeVisible()
    await expect(page.getByRole('button', { name: /New Order/ })).toBeVisible()

    await expect(table.getByRole('columnheader', { name: 'ID' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Customer' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Email' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Status' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Created' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Actions' })).toBeVisible()

    await expect(table.getByRole('row').nth(1)).toBeVisible()
  })

  test('print queue page is accessible', async ({ page }) => {
    await page.goto('/queue')
    await expect(page.getByRole('heading', { name: 'Print Queue' })).toBeVisible()
  })

  test('settings shows tabs and printer table columns', async ({ page }) => {
    await page.goto('/settings')
    const table = page.getByRole('table')
    await expect(table).toBeVisible()

    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Printers/ })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Users/ })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Profile/ })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Business/ })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Printers/ })).toHaveAttribute('aria-selected', 'true')
    await expect(page.getByRole('button', { name: /Add Printer/ })).toBeVisible()

    await expect(table.getByRole('columnheader', { name: 'Name' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Connection' })).toBeVisible()
    await expect(table.getByRole('row').nth(1)).toBeVisible()
  })

  test('printer detail page shows correct structure', async ({ page }) => {
    // Navigate via the dashboard row — avoids hardcoded IDs. The printer table
    // populates asynchronously, so wait for a printer name to render before clicking,
    // which also confirms at least one printer is connected.
    const table = page.getByRole('table')
    await expect(table.getByRole('row').nth(1).getByRole('cell').first()).not.toBeEmpty({ timeout: 15_000 })

    const firstPrinterRow = table.getByRole('row').nth(1)
    await firstPrinterRow.click()
    await expect(page).toHaveURL(/\/printer\/\d+/, { timeout: 10_000 })

    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    // Back button present
    await expect(page.getByRole('button').first()).toBeVisible()
  })

  test('job order detail page is accessible', async ({ page }) => {
    // Navigate via the job orders list — avoids hardcoded IDs
    await page.goto('/job-orders')
    const table = page.getByRole('table')
    await expect(table.getByRole('row').nth(1)).toBeVisible()

    // Click the view (first action) button on the first row
    await table.getByRole('row').nth(1).getByRole('button').first().click()
    await expect(page).toHaveURL(/\/job-orders\/\d+/, { timeout: 10_000 })

    await expect(page.getByRole('heading', { name: /Order #\d+/ })).toBeVisible()
  })

  test('filament page is accessible', async ({ page }) => {
    await page.goto('/filament')
    await expect(page.getByRole('heading', { name: 'Filament Inventory' })).toBeVisible()
  })

  test('analytics page is accessible', async ({ page }) => {
    await page.goto('/analytics')
    await expect(page.getByRole('heading', { name: 'Analytics' })).toBeVisible()
  })

  test('audit log page is accessible', async ({ page }) => {
    await page.goto('/audit')
    await expect(page.getByRole('heading', { name: 'Audit Log' })).toBeVisible()
  })
})

test('unauthenticated access to dashboard redirects to login', async ({ browser }) => {
  const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })
  const page = await context.newPage()

  await page.goto('/dashboard')
  await expect(page).toHaveURL(/\/login/, { timeout: 10_000 })
  await expect(page.getByRole('heading', { name: 'PrintHelm' })).toBeVisible()

  await context.close()
})
