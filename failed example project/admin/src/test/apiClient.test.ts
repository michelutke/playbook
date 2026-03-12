import { describe, test, expect, vi, beforeEach } from 'vitest'

// api.ts imports '$env/static/public' which is a SvelteKit virtual module.
// Stub it before importing the module under test.
vi.mock('$env/static/public', () => ({ PUBLIC_API_URL: 'http://localhost:8080' }))

const { createApiClient, ApiError } = await import('../lib/api.js')

beforeEach(() => {
  vi.resetAllMocks()
})

describe('createApiClient — clubs.list', () => {
  test('request includes Authorization header', async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([]), { status: 200 })
    )

    const client = createApiClient('my-token')
    await client.clubs.list()

    const [, init] = (global.fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer my-token')
  })

  test('401 response rejects with ApiError(401)', async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response('Unauthorized', { status: 401 })
    )

    const client = createApiClient('bad-token')
    await expect(client.clubs.list()).rejects.toMatchObject({ status: 401 })
  })

  test('rejected ApiError is instance of ApiError', async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response('Forbidden', { status: 403 })
    )

    const client = createApiClient('token')
    const err = await client.clubs.list().catch((e) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(403)
  })

  test('Content-Type header is set to application/json', async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([]), { status: 200 })
    )

    const client = createApiClient('tok')
    await client.clubs.list()

    const [, init] = (global.fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect((init.headers as Record<string, string>)['Content-Type']).toBe('application/json')
  })
})

describe('createApiClient — clubs.delete (204)', () => {
  test('204 response returns undefined', async () => {
    global.fetch = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))

    const client = createApiClient('tok')
    const result = await client.clubs.delete('club-123')
    expect(result).toBeUndefined()
  })
})

describe('createApiClient — clubs.managers.remove (204)', () => {
  test('204 response returns undefined for manager remove', async () => {
    global.fetch = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))

    const client = createApiClient('tok')
    const result = await client.clubs.managers.remove('club-1', 'manager-1')
    expect(result).toBeUndefined()
  })
})

describe('createApiClient — stats.get', () => {
  test('returns parsed JSON on 200', async () => {
    const mockStats = { totalClubs: 5, totalUsers: 100, activeEventsToday: 3, signUpsLast7Days: 12 }
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(mockStats), { status: 200 })
    )

    const client = createApiClient('tok')
    const result = await client.stats.get()
    expect(result).toEqual(mockStats)
  })
})

describe('createApiClient — auditLog.list query params', () => {
  test('builds query string from params', async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ items: [], total: 0, page: 0, pageSize: 50 }), { status: 200 })
    )

    const client = createApiClient('tok')
    await client.auditLog.list({ actorId: 'u-1', action: 'CLUB_CREATED', page: 2 })

    const [url] = (global.fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(url).toContain('actorId=u-1')
    expect(url).toContain('action=CLUB_CREATED')
    expect(url).toContain('page=2')
  })
})
