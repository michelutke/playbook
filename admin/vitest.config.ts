import { defineConfig } from 'vitest/config'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import path from 'path'

export default defineConfig({
  plugins: [svelte({ hot: false })],
  resolve: {
    conditions: ['browser'],
    alias: {
      '$app/navigation': path.resolve('./src/test/__mocks__/app-navigation.ts'),
      '$app/stores': path.resolve('./src/test/__mocks__/app-stores.ts'),
      '$app/state': path.resolve('./src/test/__mocks__/app-state.ts'),
      '$app/forms': path.resolve('./src/test/__mocks__/app-forms.ts'),
      '$app/environment': path.resolve('./src/test/__mocks__/app-environment.ts'),
      '$env/static/public': path.resolve('./src/test/__mocks__/env-public.ts'),
      '$lib': path.resolve('./src/lib'),
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{js,ts}'],
    globals: true,
  },
})
