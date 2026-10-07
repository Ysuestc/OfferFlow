import { defineConfig } from '@playwright/test'

const baseURL = process.env.OFFERFLOW_E2E_URL
if (!baseURL) throw new Error('Set OFFERFLOW_E2E_URL to an isolated local OfferFlow instance')
const url = new URL(baseURL)
if (url.protocol !== 'http:' || !['127.0.0.1', 'localhost'].includes(url.hostname) || url.pathname !== '/' || url.username || url.password) {
  throw new Error('Browser verification requires a plain loopback URL')
}
export default defineConfig({
  testDir: './tests',
  outputDir: '../target/playwright-results',
  timeout: 60000,
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'list',
  use: { baseURL, headless: true, trace: 'retain-on-failure', channel: process.env.OFFERFLOW_E2E_CHANNEL || undefined },
  projects: [
    { name: 'desktop', use: { viewport: { width: 1440, height: 1000 } } },
  ],
})
