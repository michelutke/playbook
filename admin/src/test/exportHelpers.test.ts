import { describe, test, expect } from 'vitest'
import { toCsv } from '../lib/exportHelpers.js'
import type { AuditLogEntry } from '../lib/types.js'

const HEADER = 'id,actorId,action,targetType,targetId,payload,impersonatedAs,impersonationSessionId,createdAt'

const makeEntry = (overrides: Partial<AuditLogEntry> = {}): AuditLogEntry => ({
  id: 'entry-1',
  actorId: 'actor-123',
  action: 'CLUB_CREATED',
  targetType: 'CLUB',
  targetId: 'club-abc',
  payload: null,
  impersonatedAs: null,
  impersonationSessionId: null,
  createdAt: '2024-06-15T14:30:00.000Z',
  ...overrides,
})

describe('toCsv', () => {
  test('empty array → header row only', () => {
    const result = toCsv([])
    expect(result).toBe(HEADER)
  })

  test('one entry → header + one data row', () => {
    const entry = makeEntry()
    const result = toCsv([entry])
    const lines = result.split('\n')
    expect(lines).toHaveLength(2)
    expect(lines[0]).toBe(HEADER)
    expect(lines[1]).toContain('entry-1')
    expect(lines[1]).toContain('actor-123')
    expect(lines[1]).toContain('CLUB_CREATED')
  })

  test('multiple entries → header + N data rows', () => {
    const entries = [makeEntry({ id: 'e1' }), makeEntry({ id: 'e2' }), makeEntry({ id: 'e3' })]
    const lines = toCsv(entries).split('\n')
    expect(lines).toHaveLength(4)
  })

  test('payload with commas is quoted', () => {
    const entry = makeEntry({ payload: '{"key":"value,with,commas"}' })
    const result = toCsv([entry])
    // RFC 4180: field containing commas is wrapped in quotes; internal quotes are doubled
    expect(result).toContain('"{""key"":""value,with,commas""}"')
  })

  test('payload with double-quotes escapes them', () => {
    const entry = makeEntry({ payload: 'say "hello"' })
    const result = toCsv([entry])
    // double-quotes inside a quoted field are escaped as ""
    expect(result).toContain('"say ""hello"""')
  })

  test('null fields render as empty string', () => {
    const entry = makeEntry({ targetType: null, targetId: null, payload: null })
    const result = toCsv([entry])
    const dataRow = result.split('\n')[1]
    // targetType, targetId, payload positions are all empty (consecutive commas)
    expect(dataRow).toContain(',,,')
  })
})
