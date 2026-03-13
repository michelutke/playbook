import { test, expect } from '@playwright/test'

test('unauthenticated access to protected route redirects to login', async ({ page }) => {
  await page.goto('/clubs')
  await expect(page).toHaveURL(/login/)
})

test('unauthenticated access to dashboard redirects to login', async ({ page }) => {
  await page.goto('/')
  await expect(page).toHaveURL(/login/)
})

test('login page renders email and password fields', async ({ page }) => {
  await page.goto('/login')
  await expect(page.locator('input[type="email"]')).toBeVisible()
  await expect(page.locator('input[type="password"]')).toBeVisible()
  await expect(page.locator('button[type="submit"]')).toBeVisible()
})

test('login with wrong credentials shows error', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[type="email"]', 'wrong@test.com')
  await page.fill('input[type="password"]', 'wrongpassword')
  await page.click('button[type="submit"]')
  // Error message should appear after failed login
  await expect(page.locator('[class*="red"]').first()).toBeVisible({ timeout: 5000 })
})

test('login page has correct page title', async ({ page }) => {
  await page.goto('/login')
  await expect(page).toHaveTitle(/login.*playbook|playbook.*admin/i)
})
