import { execSync } from 'child_process'
import path from 'path'

const projectRoot = path.resolve(__dirname, '../..')

export default async function globalTeardown() {
  if (process.env.CI) {
    execSync('docker compose down -v', { cwd: projectRoot, stdio: 'inherit' })
  }
  // local: leave container running for fast re-runs
}
