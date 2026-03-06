import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/svelte'
import type { SaClub } from '../lib/types.js'

vi.mock('$app/navigation', () => ({ goto: vi.fn() }))
vi.mock('$app/forms', () => ({ enhance: vi.fn(() => () => {}) }))
vi.mock('$app/state', () => ({ page: { url: new URL('http://localhost/clubs') } }))

const { default: ClubsPage } = await import('../routes/(protected)/clubs/+page.svelte')

const makeClub = (overrides: Partial<SaClub> = {}): SaClub => ({
  id: 'club-1',
  name: 'FC Test',
  status: 'ACTIVE',
  sportType: 'Football',
  location: 'Zurich',
  metadata: null,
  createdAt: '2024-01-15T10:00:00.000Z',
  managerCount: 2,
  memberCount: 25,
  teamCount: 3,
  ...overrides,
})

const makePageData = (clubs: SaClub[] = [], search = '', status = '') => ({
  clubs,
  search,
  status,
})

beforeEach(() => {
  vi.resetAllMocks()
})

describe('Clubs page', () => {
  test('renders page heading', () => {
    render(ClubsPage, { props: { data: makePageData(), form: null } })
    expect(screen.getByRole('heading', { name: /clubs/i })).toBeInTheDocument()
  })

  test('renders search input', () => {
    render(ClubsPage, { props: { data: makePageData(), form: null } })
    expect(screen.getByPlaceholderText(/search clubs/i)).toBeInTheDocument()
  })

  test('shows club count', () => {
    const clubs = [makeClub(), makeClub({ id: 'club-2', name: 'SC United' })]
    render(ClubsPage, { props: { data: makePageData(clubs), form: null } })
    expect(screen.getByText(/2 clubs/i)).toBeInTheDocument()
  })

  test('renders club rows in the table', () => {
    const clubs = [makeClub({ name: 'FC Test' }), makeClub({ id: 'club-2', name: 'SC United' })]
    render(ClubsPage, { props: { data: makePageData(clubs), form: null } })
    expect(screen.getByText('FC Test')).toBeInTheDocument()
    expect(screen.getByText('SC United')).toBeInTheDocument()
  })

  test('shows "No clubs found" when list is empty', () => {
    render(ClubsPage, { props: { data: makePageData([]), form: null } })
    expect(screen.getByText(/no clubs found/i)).toBeInTheDocument()
  })

  test('renders New Club button', () => {
    render(ClubsPage, { props: { data: makePageData(), form: null } })
    expect(screen.getByRole('button', { name: /new club/i })).toBeInTheDocument()
  })

  test('renders status filter tabs', () => {
    render(ClubsPage, { props: { data: makePageData(), form: null } })
    expect(screen.getByRole('button', { name: /^all$/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^active$/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^inactive$/i })).toBeInTheDocument()
  })

  test('renders sport type for each club', () => {
    const clubs = [makeClub({ sportType: 'Basketball' })]
    render(ClubsPage, { props: { data: makePageData(clubs), form: null } })
    expect(screen.getByText('Basketball')).toBeInTheDocument()
  })
})
