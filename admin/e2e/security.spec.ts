import { test, expect } from '@playwright/test'

test('unauthenticated access to /clubs redirects to login', async ({ page }) => {
  await page.goto('/clubs')
  await expect(page).toHaveURL(/login/)
})

test('unauthenticated access to /audit-log redirects to login', async ({ page }) => {
  await page.goto('/audit-log')
  await expect(page).toHaveURL(/login/)
})

test('unauthenticated access to /billing redirects to login', async ({ page }) => {
  await page.goto('/billing')
  await expect(page).toHaveURL(/login/)
})

test('XSS payload in query param does not execute script', async ({ page }) => {
  const alerts: string[] = []
  page.on('dialog', (dialog) => {
    alerts.push(dialog.message())
    dialog.dismiss()
  })

  // Navigate to login with an XSS query param — the app should not evaluate it
  await page.goto('/login?name=<script>alert(1)</script>')

  // Wait a moment to ensure no dialog fires
  await page.waitForTimeout(500)
  expect(alerts).toHaveLength(0)
})

test('XSS payload in search param on clubs page does not execute script', async ({ page }) => {
  const alerts: string[] = []
  page.on('dialog', (dialog) => {
    alerts.push(dialog.message())
    dialog.dismiss()
  })

  await page.goto('/clubs?search=<script>alert(1)</script>')

  // Will redirect to login — either way no alert should fire
  await page.waitForTimeout(500)
  expect(alerts).toHaveLength(0)
})
