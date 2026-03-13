import { test, expect } from '@playwright/test'

test('unauthenticated export page redirects to login', async ({ page }) => {
  await page.goto('/audit-log')
  await expect(page).toHaveURL(/login/)
})

// Authenticated export flow — requires running backend + valid SA credentials.
//
// test.describe('authenticated export', () => {
//   test.beforeEach(async ({ page }) => {
//     await page.goto('/login')
//     await page.fill('input[type="email"]', process.env.SA_EMAIL!)
//     await page.fill('input[type="password"]', process.env.SA_PASSWORD!)
//     await page.click('button[type="submit"]')
//     await page.waitForURL('/')
//   })
//
//   test('Export CSV button click triggers POST to export endpoint', async ({ page }) => {
//     await page.goto('/audit-log')
//
//     const [request] = await Promise.all([
//       page.waitForRequest((req) => req.url().includes('/api/audit-log/export') && req.method() === 'POST'),
//       page.getByRole('button', { name: /export csv/i }).click(),
//     ])
//
//     expect(request.method()).toBe('POST')
//   })
//
//   test('Export button shows loading spinner while exporting', async ({ page }) => {
//     await page.goto('/audit-log')
//     await page.route('**/api/audit-log/export', async (route) => {
//       // Delay response to observe loading state
//       await new Promise((r) => setTimeout(r, 500))
//       await route.fulfill({ json: { jobId: 'test-job', status: 'pending' } })
//     })
//
//     await page.getByRole('button', { name: /export csv/i }).click()
//     await expect(page.getByText(/exporting/i)).toBeVisible()
//   })
// })
