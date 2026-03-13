import { describe, test, expect, vi, beforeEach } from 'vitest'
import {
  saveImpersonation,
  loadImpersonation,
  clearImpersonation,
  formatCountdown,
  isExpired,
  secondsRemaining,
  type ImpersonationState,
} from '../lib/stores.js'

// Mock sessionStorage with an in-memory map
const mockStorage: Record<string, string> = {}

beforeEach(() => {
  // Clear mock storage between tests
  for (const key of Object.keys(mockStorage)) delete mockStorage[key]
  vi.restoreAllMocks()
})

vi.stubGlobal('sessionStorage', {
  getItem: (k: string) => mockStorage[k] ?? null,
  setItem: (k: string, v: string) => { mockStorage[k] = v },
  removeItem: (k: string) => { delete mockStorage[k] },
})

const makeState = (overrides: Partial<ImpersonationState> = {}): ImpersonationState => ({
  token: 'imp-token',
  sessionId: 'sess-abc',
  expiresAt: new Date(Date.now() + 30 * 60 * 1000).toISOString(), // 30 min from now
  managerId: 'mgr-1',
  managerEmail: 'manager@club.com',
  clubId: 'club-1',
  ...overrides,
})

describe('saveImpersonation / loadImpersonation', () => {
  test('load returns null when nothing saved', () => {
    expect(loadImpersonation()).toBeNull()
  })

  test('save then load returns the same state', () => {
    const state = makeState()
    saveImpersonation(state)
    expect(loadImpersonation()).toEqual(state)
  })

  test('overwrites previous state on second save', () => {
    saveImpersonation(makeState({ managerId: 'mgr-1' }))
    saveImpersonation(makeState({ managerId: 'mgr-2' }))
    expect(loadImpersonation()?.managerId).toBe('mgr-2')
  })
})

describe('clearImpersonation', () => {
  test('clearImpersonation → loadImpersonation returns null', () => {
    saveImpersonation(makeState())
    clearImpersonation()
    expect(loadImpersonation()).toBeNull()
  })

  test('clearImpersonation is safe to call when nothing saved', () => {
    expect(() => clearImpersonation()).not.toThrow()
  })
})

describe('isExpired', () => {
  test('past date → true', () => {
    const past = new Date(Date.now() - 1000).toISOString()
    expect(isExpired(past)).toBe(true)
  })

  test('future date → false', () => {
    const future = new Date(Date.now() + 60_000).toISOString()
    expect(isExpired(future)).toBe(false)
  })
})

describe('formatCountdown', () => {
  test('future date → matches MM:SS format', () => {
    const future = new Date(Date.now() + 5 * 60 * 1000 + 30 * 1000).toISOString() // 5m30s
    const result = formatCountdown(future)
    expect(result).toMatch(/^\d{2}:\d{2}$/)
  })

  test('past/expired date → 00:00', () => {
    const past = new Date(Date.now() - 1000).toISOString()
    expect(formatCountdown(past)).toBe('00:00')
  })

  test('exactly 1 minute → 01:00', () => {
    // Add a small buffer to account for execution time
    const future = new Date(Date.now() + 60_000 + 500).toISOString()
    const result = formatCountdown(future)
    // Should be 01:00 or 00:59 depending on exact timing
    expect(result).toMatch(/^0[01]:\d{2}$/)
  })
})

describe('secondsRemaining', () => {
  test('future date → positive number', () => {
    const future = new Date(Date.now() + 120_000).toISOString()
    expect(secondsRemaining(future)).toBeGreaterThan(0)
  })

  test('past date → 0', () => {
    const past = new Date(Date.now() - 5000).toISOString()
    expect(secondsRemaining(past)).toBe(0)
  })

  test('2-minute future → approximately 120 seconds', () => {
    const future = new Date(Date.now() + 120_000).toISOString()
    const secs = secondsRemaining(future)
    expect(secs).toBeGreaterThanOrEqual(119)
    expect(secs).toBeLessThanOrEqual(120)
  })
})
