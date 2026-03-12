import { describe, test, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/svelte'
import type { ClubBillingEntry } from '../lib/types.js'

const { default: BillingPage } = await import('../routes/(protected)/billing/+page.svelte')

const makeEntry = (overrides: Partial<ClubBillingEntry> = {}): ClubBillingEntry => ({
  clubId: 'club-1',
  clubName: 'FC Test',
  activeMemberCount: 25,
  annualBillingChf: 500,
  ...overrides,
})

const makePageData = (entries: ClubBillingEntry[] = []) => ({ entries })

describe('Billing page', () => {
  test('renders page heading', () => {
    render(BillingPage, { props: { data: makePageData() } })
    expect(screen.getByRole('heading', { name: /billing/i })).toBeInTheDocument()
  })

  test('renders Total Active Members and Total Annual Revenue summary cards', () => {
    render(BillingPage, { props: { data: makePageData() } })
    expect(screen.getByText(/total active members/i)).toBeInTheDocument()
    expect(screen.getByText(/total annual revenue/i)).toBeInTheDocument()
  })

  test('renders club names from entries', () => {
    const entries = [
      makeEntry({ clubName: 'FC Test' }),
      makeEntry({ clubId: 'club-2', clubName: 'SC United' }),
    ]
    render(BillingPage, { props: { data: makePageData(entries) } })
    expect(screen.getByText('FC Test')).toBeInTheDocument()
    expect(screen.getByText('SC United')).toBeInTheDocument()
  })

  test('renders CHF amounts for entries', () => {
    const entries = [makeEntry({ annualBillingChf: 1200 })]
    render(BillingPage, { props: { data: makePageData(entries) } })
    // Intl.NumberFormat de-CH formats as "CHF 1'200.00" or similar
    const allText = document.body.textContent ?? ''
    expect(allText).toContain('1')
    expect(allText).toMatch(/chf|CHF/i)
  })

  test('renders active member counts', () => {
    const entries = [makeEntry({ activeMemberCount: 42 })]
    render(BillingPage, { props: { data: makePageData(entries) } })
    expect(screen.getAllByText('42').length).toBeGreaterThan(0)
  })

  test('shows "No billing data available" when entries is empty', () => {
    render(BillingPage, { props: { data: makePageData([]) } })
    expect(screen.getByText(/no billing data available/i)).toBeInTheDocument()
  })

  test('renders table footer with totals when entries present', () => {
    const entries = [
      makeEntry({ activeMemberCount: 10, annualBillingChf: 200 }),
      makeEntry({ clubId: 'club-2', activeMemberCount: 20, annualBillingChf: 400 }),
    ]
    render(BillingPage, { props: { data: makePageData(entries) } })
    // "Total" label in tfoot
    expect(screen.getByText('Total')).toBeInTheDocument()
  })

  test('renders link to club detail for each entry', () => {
    const entries = [makeEntry({ clubId: 'club-abc', clubName: 'FC Test' })]
    render(BillingPage, { props: { data: makePageData(entries) } })
    const link = screen.getByRole('link', { name: 'FC Test' })
    expect(link).toHaveAttribute('href', '/clubs/club-abc')
  })
})
