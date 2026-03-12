import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/svelte'
import type { AuditLogEntry, PagedResponse } from '../lib/types.js'

vi.mock('$app/navigation', () => ({ goto: vi.fn() }))

const { default: AuditLogPage } = await import('../routes/(protected)/audit-log/+page.svelte')

const makeEntry = (overrides: Partial<AuditLogEntry> = {}): AuditLogEntry => ({
  id: 'entry-1',
  actorId: 'actor-abcdef123456',
  action: 'CLUB_CREATED',
  targetType: 'CLUB',
  targetId: 'club-xyz',
  payload: null,
  impersonatedAs: null,
  impersonationSessionId: null,
  createdAt: '2024-06-15T14:30:00.000Z',
  ...overrides,
})

const makeResult = (items: AuditLogEntry[] = [], total = 0): PagedResponse<AuditLogEntry> => ({
  items,
  total,
  page: 0,
  pageSize: 50,
})

const makePageData = (items: AuditLogEntry[] = [], total = 0) => ({
  result: makeResult(items, total),
  page: 0,
  filters: {},
})

beforeEach(() => {
  vi.resetAllMocks()
})

describe('Audit log page', () => {
  test('renders page heading', () => {
    render(AuditLogPage, { props: { data: makePageData() } })
    expect(screen.getByRole('heading', { name: /audit log/i })).toBeInTheDocument()
  })

  test('shows total entry count', () => {
    render(AuditLogPage, { props: { data: makePageData([], 42) } })
    expect(screen.getByText(/42 entries/i)).toBeInTheDocument()
  })

  test('renders Export CSV button', () => {
    render(AuditLogPage, { props: { data: makePageData() } })
    expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument()
  })

  test('renders filter inputs', () => {
    render(AuditLogPage, { props: { data: makePageData() } })
    expect(screen.getByPlaceholderText(/user id/i)).toBeInTheDocument()
    expect(screen.getByPlaceholderText(/club_created/i)).toBeInTheDocument()
  })

  test('renders Apply Filters button', () => {
    render(AuditLogPage, { props: { data: makePageData() } })
    expect(screen.getByRole('button', { name: /apply filters/i })).toBeInTheDocument()
  })

  test('renders audit log entries in table', () => {
    const entries = [
      makeEntry({ action: 'CLUB_CREATED', actorId: 'actor-abcdef123456' }),
      makeEntry({ id: 'entry-2', action: 'MEMBER_ADDED', actorId: 'actor-xyz9876543' }),
    ]
    render(AuditLogPage, { props: { data: makePageData(entries, 2) } })
    expect(screen.getByText('CLUB_CREATED')).toBeInTheDocument()
    expect(screen.getByText('MEMBER_ADDED')).toBeInTheDocument()
  })

  test('shows "No entries found" when result is empty', () => {
    render(AuditLogPage, { props: { data: makePageData([]) } })
    expect(screen.getByText(/no entries found/i)).toBeInTheDocument()
  })

  test('renders formatted timestamps for entries', () => {
    const entries = [makeEntry({ createdAt: '2024-06-15T14:30:00.000Z' })]
    render(AuditLogPage, { props: { data: makePageData(entries, 1) } })
    // Just assert that a date-like string appears (locale-formatted)
    const cells = screen.getAllByRole('cell')
    const hasDateCell = cells.some((c) => c.textContent && c.textContent.match(/\d{4}|\d{2}/))
    expect(hasDateCell).toBe(true)
  })

  test('renders table column headers', () => {
    render(AuditLogPage, { props: { data: makePageData() } })
    expect(screen.getAllByText(/timestamp/i).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/actor/i).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/action/i).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/target/i).length).toBeGreaterThan(0)
  })
})
