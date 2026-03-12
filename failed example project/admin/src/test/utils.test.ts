import { describe, test, expect } from 'vitest'
import { formatDate, formatDateOnly, statusColor, truncate } from '../lib/utils.js'

const ISO = '2024-06-15T14:30:00.000Z'

describe('formatDate', () => {
  test('returns a non-empty string for valid ISO date', () => {
    const result = formatDate(ISO)
    expect(result).toBeTruthy()
    expect(typeof result).toBe('string')
  })

  test('includes time component', () => {
    const result = formatDate(ISO)
    // en-CH medium dateStyle + short timeStyle produces something like "15 Jun 2024, 16:30"
    expect(result.length).toBeGreaterThan(10)
  })
})

describe('formatDateOnly', () => {
  test('returns a non-empty string for valid ISO date', () => {
    const result = formatDateOnly(ISO)
    expect(result).toBeTruthy()
    expect(typeof result).toBe('string')
  })

  test('is shorter than formatDate (no time component)', () => {
    const withTime = formatDate(ISO)
    const dateOnly = formatDateOnly(ISO)
    expect(dateOnly.length).toBeLessThan(withTime.length)
  })

  test('does not contain a colon (no time part)', () => {
    const result = formatDateOnly(ISO)
    expect(result).not.toContain(':')
  })
})

describe('statusColor', () => {
  test('active → contains emerald', () => {
    expect(statusColor('active')).toContain('emerald')
  })

  test('ACTIVE (uppercase) → contains emerald', () => {
    expect(statusColor('ACTIVE')).toContain('emerald')
  })

  test('inactive → contains zinc', () => {
    expect(statusColor('inactive')).toContain('zinc')
  })

  test('deactivated → contains zinc', () => {
    expect(statusColor('deactivated')).toContain('zinc')
  })

  test('pending → contains amber', () => {
    expect(statusColor('pending')).toContain('amber')
  })

  test('rejected → contains red', () => {
    expect(statusColor('rejected')).toContain('red')
  })

  test('unknown status → falls back to zinc', () => {
    expect(statusColor('unknown')).toContain('zinc')
  })
})

describe('truncate', () => {
  test('string shorter than limit is returned unchanged', () => {
    expect(truncate('hi', 5)).toBe('hi')
  })

  test('string equal to limit is returned unchanged', () => {
    expect(truncate('hello', 5)).toBe('hello')
  })

  test('string longer than limit is truncated with ellipsis', () => {
    // utils.ts uses the Unicode ellipsis character '…' (U+2026)
    expect(truncate('hello world', 5)).toBe('hello\u2026')
  })

  test('truncates to the exact character count before ellipsis', () => {
    const result = truncate('abcdefgh', 3)
    expect(result).toBe('abc\u2026')
  })
})
