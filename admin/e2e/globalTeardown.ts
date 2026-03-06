import { execSync } from 'child_process'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const projectRoot = path.resolve(__dirname, '../..')

export default async function globalTeardown() {
  if (process.env.CI) {
    execSync('docker compose down -v', { cwd: projectRoot, stdio: 'inherit' })
  }
  // local: leave container running for fast re-runs
}
