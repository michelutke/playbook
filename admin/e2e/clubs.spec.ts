import { test, expect } from '@playwright/test'

// These tests run against the dev server. Without a valid SA session
// they will redirect to login — which is expected and tested here.
// Full authenticated flows require a running backend + valid credentials.

test('unauthenticated clubs page redirects to login', async ({ page }) => {
  await page.goto('/clubs')
  await expect(page).toHaveURL(/login/)
})

test('unauthenticated club detail page redirects to login', async ({ page }) => {
  await page.goto('/clubs/some-club-id')
  await expect(page).toHaveURL(/login/)
})

// Authenticated flow — skipped when backend is unavailable.
// Uncomment and configure credentials to run against a live environment.
//
// test.describe('authenticated clubs', () => {
//   test.beforeEach(async ({ page }) => {
//     await page.goto('/login')
//     await page.fill('input[type="email"]', process.env.SA_EMAIL!)
//     await page.fill('input[type="password"]', process.env.SA_PASSWORD!)
//     await page.click('button[type="submit"]')
//     await page.waitForURL('/')
//   })
//
//   test('clubs page shows search input', async ({ page }) => {
//     await page.goto('/clubs')
//     await expect(page.locator('input[placeholder*="Search"]')).toBeVisible()
//   })
//
//   test('clicking first club navigates to detail page', async ({ page }) => {
//     await page.goto('/clubs')
//     await page.locator('tbody tr').first().click()
//     await expect(page).toHaveURL(/\/clubs\/[^/]+$/)
//   })
// })
