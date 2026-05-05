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
      const rewritten = route.request().url().replace(externalApiOrigin, internalApiUrl)
      const response = await route.fetch({ url: rewritten })
      await route.fulfill({ response })
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

  test('job orders lists correct columns and at least one row', async ({ page }) => {
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
    await expect(table.getByRole('cell', { name: 'Creed Zagrzebski' }).first()).toBeVisible()
    await expect(table.getByRole('cell', { name: 'czagrzebski@gmail.com' }).first()).toBeVisible()
    await expect(table.getByRole('cell', { name: /Ready to Print/ }).first()).toBeVisible()
  })

  test('print queue shows printer card and queue table columns', async ({ page }) => {
    await page.goto('/queue')
    await expect(page.getByRole('heading', { name: 'Print Queue' })).toBeVisible()

    const table = page.getByRole('table').first()
    await expect(table).toBeVisible()

    await expect(table.getByRole('columnheader', { name: '#' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Job' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Customer' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'File' })).toBeVisible()

    await expect(table.getByRole('row').nth(1)).toBeVisible()
    await expect(table.getByRole('cell', { name: 'Creed Zagrzebski' }).first()).toBeVisible()
    await expect(page.getByText('mithradiccoin.gcode.3mf').first()).toBeVisible()
  })

  test('settings shows tabs and printer table columns', async ({ page }) => {
    await page.goto('/settings')
    const table = page.getByRole('table')
    await expect(table).toBeVisible()

    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Printers/ })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Users/ })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Profile/ })).toBeVisible()
    await expect(page.getByRole('tab', { name: /Printers/ })).toHaveAttribute('aria-selected', 'true')
    await expect(page.getByRole('button', { name: /Add Printer/ })).toBeVisible()

    await expect(table.getByRole('columnheader', { name: 'Name' })).toBeVisible()
    await expect(table.getByRole('columnheader', { name: 'Connection' })).toBeVisible()
    await expect(table.getByRole('row').nth(1)).toBeVisible()
    await expect(table.getByText(/ssl:\/\//).first()).toBeVisible()
  })

  test('printer detail page shows correct structure', async ({ page }) => {
    // Navigate via the dashboard row — avoids hardcoded IDs
    const firstPrinterRow = page.getByRole('table').getByRole('row').nth(1)
    await expect(firstPrinterRow).toBeVisible()
    await firstPrinterRow.click()
    await expect(page).toHaveURL(/\/printer\/\d+/, { timeout: 10_000 })

    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    // Back button present
    await expect(page.getByRole('button').first()).toBeVisible()
  })

  test('job order detail shows order info and workflow stepper', async ({ page }) => {
    // Navigate via the job orders list — avoids hardcoded IDs
    await page.goto('/job-orders')
    const table = page.getByRole('table')
    await expect(table.getByRole('row').nth(1)).toBeVisible()

    // Click the view (first action) button on the first row
    await table.getByRole('row').nth(1).getByRole('button').first().click()
    await expect(page).toHaveURL(/\/job-orders\/\d+/, { timeout: 10_000 })

    await expect(page.getByRole('heading', { name: /Order #\d+/ })).toBeVisible()

    await expect(page.getByRole('button', { name: /Submitted/ })).toBeVisible()
    await expect(page.getByRole('button', { name: /Review/ })).toBeVisible()
    await expect(page.getByRole('button', { name: /Ready to Print/ })).toBeVisible()
    await expect(page.getByRole('button', { name: /Printing/ })).toBeVisible()
    await expect(page.getByRole('button', { name: /Finished/ })).toBeVisible()

    await expect(page.getByText('Print Queue Assignment')).toBeVisible()
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
