import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte'

// SvelteKit virtual modules must be mocked before importing the page component
vi.mock('$app/navigation', () => ({ goto: vi.fn() }))
vi.mock('$app/forms', () => ({ enhance: vi.fn(() => () => {}) }))

const { default: LoginPage } = await import('../routes/login/+page.svelte')

beforeEach(() => {
  vi.resetAllMocks()
  global.fetch = vi.fn()
})

describe('Login page', () => {
  test('renders email and password fields', () => {
    render(LoginPage)
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
  })

  test('renders submit button', () => {
    render(LoginPage)
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  test('renders heading and subtitle', () => {
    render(LoginPage)
    expect(screen.getByText('Playbook')).toBeInTheDocument()
    expect(screen.getByText(/super admin panel/i)).toBeInTheDocument()
  })

  test('shows loading state while submitting', async () => {
    // fetch resolves after a delay so we can observe the loading state
    global.fetch = vi.fn().mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(new Response(JSON.stringify({}), { status: 200 })), 100))
    )

    render(LoginPage)

    const emailInput = screen.getByLabelText(/email address/i)
    const passwordInput = screen.getByLabelText(/password/i)
    const form = emailInput.closest('form')!

    await fireEvent.input(emailInput, { target: { value: 'admin@playbook.com' } })
    await fireEvent.input(passwordInput, { target: { value: 'secret' } })
    fireEvent.submit(form)

    await waitFor(() => {
      expect(screen.getByText(/signing in/i)).toBeInTheDocument()
    })
  })

  test('shows error message on failed login', async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ error: 'Ungültige Anmeldedaten' }), { status: 401 })
    )

    render(LoginPage)

    const emailInput = screen.getByLabelText(/email address/i)
    const passwordInput = screen.getByLabelText(/password/i)
    const form = emailInput.closest('form')!

    await fireEvent.input(emailInput, { target: { value: 'wrong@test.com' } })
    await fireEvent.input(passwordInput, { target: { value: 'wrongpassword' } })
    await fireEvent.submit(form)

    await waitFor(() => {
      expect(screen.getByText(/ungültige anmeldedaten/i)).toBeInTheDocument()
    })
  })

  test('shows network error on fetch failure', async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error('Network error'))

    render(LoginPage)

    const form = screen.getByLabelText(/email address/i).closest('form')!
    await fireEvent.submit(form)

    await waitFor(() => {
      expect(screen.getByText(/network error/i)).toBeInTheDocument()
    })
  })
})
