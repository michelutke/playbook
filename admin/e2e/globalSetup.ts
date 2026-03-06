import { execSync } from 'child_process'
import path from 'path'

const projectRoot = path.resolve(__dirname, '../..')

export default async function globalSetup() {
  console.log('[E2E] Starting postgres via docker compose...')
  execSync('docker compose up -d --wait db', {
    cwd: projectRoot,
    stdio: 'inherit',
  })
  console.log('[E2E] Postgres ready on port 5433')
}
