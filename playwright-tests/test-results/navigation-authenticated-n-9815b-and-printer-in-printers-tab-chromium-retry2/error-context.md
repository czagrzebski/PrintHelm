# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: navigation.spec.ts >> authenticated navigation >> settings shows tabs and printer in printers tab
- Location: tests/navigation.spec.ts:103:7

# Error details

```
Error: expect(page).toHaveURL(expected) failed

Expected pattern: /\/dashboard/
Received string:  "http://localhost:5173/login"
Timeout: 15000ms

Call log:
  - Expect "toHaveURL" with timeout 15000ms
    19 × unexpected value "http://localhost:5173/login"

```

# Page snapshot

```yaml
- generic [active] [ref=e1]:
  - generic [ref=e5]:
    - generic [ref=e6]:
      - generic [ref=e8]: 󰐫
      - heading "PrintHelm" [level=1] [ref=e9]
      - paragraph [ref=e10]: Sign in to your account
    - generic [ref=e12]:
      - alert [ref=e13]:
        - generic [ref=e15]: Invalid username or password.
      - generic [ref=e16]:
        - generic [ref=e17]: Username
        - textbox "Username" [ref=e18]:
          - /placeholder: Enter username
          - text: admin
      - generic [ref=e19]:
        - generic [ref=e20]: Password
        - generic [ref=e21]:
          - textbox "Enter password" [ref=e22]: printhelm
          - img [ref=e23]
          - generic: Enter a password
      - button "Sign In" [ref=e25] [cursor=pointer]:
        - generic [ref=e26]: 
        - generic [ref=e27]: Sign In
  - generic [ref=e28]:
    - generic "Toggle devtools panel" [ref=e29] [cursor=pointer]:
      - img [ref=e30]
    - generic "Toggle Component Inspector" [ref=e35] [cursor=pointer]:
      - img [ref=e36]
```

# Test source

```ts
  1   | import { test, expect, type Page } from '@playwright/test'
  2   | 
  3   | async function login(page: Page) {
  4   |   const username = process.env.PLAYWRIGHT_USERNAME
  5   |   const password = process.env.PLAYWRIGHT_PASSWORD
  6   | 
  7   |   if (!username || !password) {
  8   |     throw new Error('PLAYWRIGHT_USERNAME and PLAYWRIGHT_PASSWORD must be set')
  9   |   }
  10  | 
  11  |   const externalApiOrigin = process.env.PLAYWRIGHT_EXTERNAL_API_ORIGIN
  12  |   const internalApiUrl = process.env.PLAYWRIGHT_INTERNAL_API_URL
  13  | 
  14  |   if (externalApiOrigin && internalApiUrl) {
  15  |     await page.route(`${externalApiOrigin}/**`, async (route) => {
  16  |       const rewritten = route.request().url().replace(externalApiOrigin, internalApiUrl)
  17  |       const response = await route.fetch({ url: rewritten })
  18  |       await route.fulfill({ response })
  19  |     })
  20  |   }
  21  | 
  22  |   await page.goto('/login')
  23  |   await page.fill('#username', username)
  24  |   await page.fill('input[autocomplete="current-password"]', password)
  25  |   await page.click('button[type="submit"]')
> 26  |   await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 })
      |                      ^ Error: expect(page).toHaveURL(expected) failed
  27  | }
  28  | 
  29  | test.describe('authenticated navigation', () => {
  30  |   test.beforeEach(async ({ page }) => {
  31  |     await login(page)
  32  |   })
  33  | 
  34  |   test('sidebar renders all nav links', async ({ page }) => {
  35  |     const nav = page.getByRole('navigation')
  36  |     await expect(nav.getByRole('link', { name: /Dashboard/ })).toHaveAttribute('href', '/dashboard')
  37  |     await expect(nav.getByRole('link', { name: /Job Orders/ })).toHaveAttribute('href', '/job-orders')
  38  |     await expect(nav.getByRole('link', { name: /Print Queue/ })).toHaveAttribute('href', '/queue')
  39  |     await expect(nav.getByRole('link', { name: /Settings/ })).toHaveAttribute('href', '/settings')
  40  |   })
  41  | 
  42  |   test('dashboard shows stat cards and printer table', async ({ page }) => {
  43  |     await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
  44  | 
  45  |     // Stat cards
  46  |     await expect(page.getByText('Total Printers')).toBeVisible()
  47  |     await expect(page.getByText('Printing')).toBeVisible()
  48  |     await expect(page.getByText('Idle')).toBeVisible()
  49  |     await expect(page.getByText('Offline')).toBeVisible()
  50  | 
  51  |     // Printers section
  52  |     await expect(page.getByRole('heading', { name: 'Printers', level: 1 }).or(page.getByText('Printers').first())).toBeVisible()
  53  | 
  54  |     const table = page.getByRole('table')
  55  |     await expect(table.getByRole('columnheader', { name: 'Name' })).toBeVisible()
  56  |     await expect(table.getByRole('columnheader', { name: 'Status' })).toBeVisible()
  57  |     await expect(table.getByRole('columnheader', { name: 'Progress' })).toBeVisible()
  58  | 
  59  |     // Seeded printer row (status cell omitted — depends on live MQTT data)
  60  |     await expect(table.getByRole('row', { name: /Bambulab X1C/ })).toBeVisible()
  61  |   })
  62  | 
  63  |   test('job orders lists orders with correct columns and seeded row', async ({ page }) => {
  64  |     await page.goto('/job-orders')
  65  |     await expect(page.getByRole('heading', { name: 'Job Orders' })).toBeVisible()
  66  |     await expect(page.getByRole('button', { name: /New Order/ })).toBeVisible()
  67  | 
  68  |     const table = page.getByRole('table')
  69  |     await expect(table.getByRole('columnheader', { name: 'ID' })).toBeVisible()
  70  |     await expect(table.getByRole('columnheader', { name: 'Customer' })).toBeVisible()
  71  |     await expect(table.getByRole('columnheader', { name: 'Email' })).toBeVisible()
  72  |     await expect(table.getByRole('columnheader', { name: 'Status' })).toBeVisible()
  73  |     await expect(table.getByRole('columnheader', { name: 'Created' })).toBeVisible()
  74  |     await expect(table.getByRole('columnheader', { name: 'Actions' })).toBeVisible()
  75  | 
  76  |     // Seeded order row
  77  |     await expect(table.getByRole('cell', { name: 'Creed Zagrzebski' })).toBeVisible()
  78  |     await expect(table.getByRole('cell', { name: 'czagrzebski@gmail.com' })).toBeVisible()
  79  |     await expect(table.getByRole('cell', { name: /Ready to Print/ })).toBeVisible()
  80  |   })
  81  | 
  82  |   test('print queue shows printer card and queued job', async ({ page }) => {
  83  |     await page.goto('/queue')
  84  |     await expect(page.getByRole('heading', { name: 'Print Queue' })).toBeVisible()
  85  | 
  86  |     // Printer card header
  87  |     await expect(page.getByText('Bambulab X1C')).toBeVisible()
  88  |     await expect(page.getByText('X1 Carbon')).toBeVisible()
  89  | 
  90  |     // Queue table columns
  91  |     const table = page.getByRole('table')
  92  |     await expect(table.getByRole('columnheader', { name: '#' })).toBeVisible()
  93  |     await expect(table.getByRole('columnheader', { name: 'Job' })).toBeVisible()
  94  |     await expect(table.getByRole('columnheader', { name: 'Customer' })).toBeVisible()
  95  |     await expect(table.getByRole('columnheader', { name: 'File' })).toBeVisible()
  96  | 
  97  |     // Seeded queue entry
  98  |     await expect(table.getByRole('cell', { name: 'Creed Zagrzebski' })).toBeVisible()
  99  |     await expect(table.getByText('mithradiccoin.gcode.3mf')).toBeVisible()
  100 |   })
  101 | 
  102 |   test('settings shows tabs and printer in printers tab', async ({ page }) => {
  103 |     await page.goto('/settings')
  104 |     await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible()
  105 | 
  106 |     // All three tabs present
  107 |     await expect(page.getByRole('tab', { name: /Printers/ })).toBeVisible()
  108 |     await expect(page.getByRole('tab', { name: /Users/ })).toBeVisible()
  109 |     await expect(page.getByRole('tab', { name: /Profile/ })).toBeVisible()
  110 | 
  111 |     // Printers tab is selected by default and shows the printer table
  112 |     await expect(page.getByRole('tab', { name: /Printers/ })).toHaveAttribute('aria-selected', 'true')
  113 |     await expect(page.getByRole('button', { name: /Add Printer/ })).toBeVisible()
  114 | 
  115 |     const table = page.getByRole('table')
  116 |     await expect(table.getByRole('columnheader', { name: 'Name' })).toBeVisible()
  117 |     await expect(table.getByRole('columnheader', { name: 'Connection' })).toBeVisible()
  118 |     await expect(table.getByRole('cell', { name: 'Bambulab X1C' })).toBeVisible()
  119 |     await expect(table.getByText(/ssl:\/\//)).toBeVisible()
  120 |   })
  121 | 
  122 |   test('printer detail page shows name and serial number', async ({ page }) => {
  123 |     await page.goto('/printer/1')
  124 |     await expect(page.getByRole('heading', { name: 'Bambulab X1C' })).toBeVisible()
  125 |     await expect(page.getByText('X1 Carbon')).toBeVisible()
  126 |     await expect(page.getByText('00M09D461600890')).toBeVisible()
```