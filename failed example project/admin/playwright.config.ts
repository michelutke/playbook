import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/globalSetup.ts',
  globalTeardown: './e2e/globalTeardown.ts',
  use: {
    baseURL: 'http://localhost:5173',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      // Ktor backend — postgres must be up (started by globalSetup)
      command: [
        'DATABASE_URL=jdbc:postgresql://localhost:5433/playbook_e2e',
        'DATABASE_USER=playbook',
        'DATABASE_PASSWORD=playbook',
        'JWT_SECRET=e2e-test-secret-not-for-production',
        'SA_PASSWORD=e2e-sa-password',
        'SMTP_HOST=localhost',
        'SMTP_PORT=1025',
        'CORS_ALLOWED_HOST=localhost:5173',
        'PORT=8088',
        './gradlew :server:run',
      ].join(' '),
      url: 'http://localhost:8088/health',
      timeout: 120_000,
      reuseExistingServer: !process.env.CI,
      cwd: '..',
    },
    {
      command: 'PUBLIC_API_URL=http://localhost:8088 npm run dev',
      url: 'http://localhost:5173',
      reuseExistingServer: !process.env.CI,
    },
  ],
})
