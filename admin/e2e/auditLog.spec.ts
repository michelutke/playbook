import { test, expect } from '@playwright/test'

test('unauthenticated audit log page redirects to login', async ({ page }) => {
  await page.goto('/audit-log')
  await expect(page).toHaveURL(/login/)
})

// Authenticated flow — requires running backend + valid SA credentials.
//
// test.describe('authenticated audit log', () => {
//   test.beforeEach(async ({ page }) => {
//     await page.goto('/login')
//     await page.fill('input[type="email"]', process.env.SA_EMAIL!)
//     await page.fill('input[type="password"]', process.env.SA_PASSWORD!)
//     await page.click('button[type="submit"]')
//     await page.waitForURL('/')
//   })
//
//   test('audit log table is visible', async ({ page }) => {
//     await page.goto('/audit-log')
//     await expect(page.locator('table')).toBeVisible()
//   })
//
//   test('Export CSV button is visible', async ({ page }) => {
//     await page.goto('/audit-log')
//     await expect(page.getByRole('button', { name: /export csv/i })).toBeVisible()
//   })
// })
