import { describe, test, expect } from 'vitest'
import { roleLabel } from '../lib/roleLabel.js'

describe('roleLabel', () => {
  test('coach → Coach', () => {
    expect(roleLabel('coach')).toBe('Coach')
  })

  test('COACH (uppercase) → Coach', () => {
    expect(roleLabel('COACH')).toBe('Coach')
  })

  test('player → Player', () => {
    expect(roleLabel('player')).toBe('Player')
  })

  test('admin → Admin', () => {
    expect(roleLabel('admin')).toBe('Admin')
  })

  test('manager → Manager', () => {
    expect(roleLabel('manager')).toBe('Manager')
  })

  test('unknown role → capitalised verbatim', () => {
    expect(roleLabel('owner')).toBe('Owner')
  })
})
