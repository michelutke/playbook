import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/svelte'
import type { SaClub, SaManager } from '../lib/types.js'

vi.mock('$app/navigation', () => ({ goto: vi.fn() }))
vi.mock('$app/forms', () => ({ enhance: vi.fn(() => () => {}) }))

// stores.ts uses sessionStorage — stub it
const mockStorage: Record<string, string> = {}
vi.stubGlobal('sessionStorage', {
  getItem: (k: string) => mockStorage[k] ?? null,
  setItem: (k: string, v: string) => { mockStorage[k] = v },
  removeItem: (k: string) => { delete mockStorage[k] },
})

const { default: ClubDetailPage } = await import('../routes/(protected)/clubs/[id]/+page.svelte')

const makeClub = (overrides: Partial<SaClub> = {}): SaClub => ({
  id: 'club-1',
  name: 'FC Test',
  status: 'ACTIVE',
  sportType: 'Football',
  location: 'Zurich',
  metadata: null,
  createdAt: '2024-01-15T10:00:00.000Z',
  managerCount: 1,
  memberCount: 25,
  teamCount: 3,
  ...overrides,
})

const makeManager = (overrides: Partial<SaManager> = {}): SaManager => ({
  id: 'mgr-1',
  clubId: 'club-1',
  userId: 'user-1',
  invitedEmail: 'coach@club.com',
  displayName: 'Coach Hans',
  status: 'ACTIVE',
  addedAt: '2024-01-16T10:00:00.000Z',
  acceptedAt: '2024-01-17T10:00:00.000Z',
  ...overrides,
})

const makePageData = (club: SaClub, managers: SaManager[] = []) => ({ club, managers })

beforeEach(() => {
  for (const k of Object.keys(mockStorage)) delete mockStorage[k]
  vi.resetAllMocks()
})

describe('Club detail page', () => {
  test('renders club name as heading', () => {
    render(ClubDetailPage, { props: { data: makePageData(makeClub()), form: null } })
    expect(screen.getByRole('heading', { name: /fc test/i })).toBeInTheDocument()
  })

  test('renders club status badge', () => {
    render(ClubDetailPage, { props: { data: makePageData(makeClub({ status: 'ACTIVE' })), form: null } })
    expect(screen.getAllByText('ACTIVE').length).toBeGreaterThan(0)
  })

  test('renders sport type', () => {
    render(ClubDetailPage, { props: { data: makePageData(makeClub({ sportType: 'Basketball' })), form: null } })
    expect(screen.getByText('Basketball')).toBeInTheDocument()
  })

  test('renders stat counts (teams, members, managers)', () => {
    const club = makeClub({ teamCount: 4, memberCount: 32, managerCount: 2 })
    render(ClubDetailPage, { props: { data: makePageData(club), form: null } })
    expect(screen.getByText('4')).toBeInTheDocument()
    expect(screen.getByText('32')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  test('renders manager list', () => {
    const managers = [
      makeManager({ displayName: 'Coach Hans', invitedEmail: 'hans@club.com' }),
      makeManager({ id: 'mgr-2', displayName: 'Coach Sara', invitedEmail: 'sara@club.com' }),
    ]
    render(ClubDetailPage, { props: { data: makePageData(makeClub(), managers), form: null } })
    expect(screen.getByText('Coach Hans')).toBeInTheDocument()
    expect(screen.getByText('Coach Sara')).toBeInTheDocument()
  })

  test('shows no-managers warning when managers list is empty', () => {
    render(ClubDetailPage, { props: { data: makePageData(makeClub(), []), form: null } })
    expect(screen.getByText(/no managers/i)).toBeInTheDocument()
  })

  test('renders Invite Manager button', () => {
    render(ClubDetailPage, { props: { data: makePageData(makeClub()), form: null } })
    expect(screen.getByRole('button', { name: /invite manager/i })).toBeInTheDocument()
  })

  test('renders breadcrumb link back to clubs', () => {
    render(ClubDetailPage, { props: { data: makePageData(makeClub()), form: null } })
    expect(screen.getByRole('link', { name: /clubs/i })).toBeInTheDocument()
  })

  test('renders Danger Zone section', () => {
    render(ClubDetailPage, { props: { data: makePageData(makeClub()), form: null } })
    expect(screen.getByText(/danger zone/i)).toBeInTheDocument()
  })
})
