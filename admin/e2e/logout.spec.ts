import { test, expect } from '@playwright/test'

test('after logout (cleared cookie) accessing protected page redirects to login', async ({ page, context }) => {
  // Clear all cookies to simulate a logged-out state
  await context.clearCookies()

  await page.goto('/clubs')
  await expect(page).toHaveURL(/login/)
})

test('login page is accessible without authentication', async ({ page, context }) => {
  await context.clearCookies()
  await page.goto('/login')
  await expect(page).toHaveURL(/login/)
  await expect(page.locator('h1')).toContainText('Playbook')
})
