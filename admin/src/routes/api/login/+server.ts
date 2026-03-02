import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types.js';

const API_URL = process.env.PUBLIC_API_URL ?? 'http://localhost:8080';
const COOKIE_MAX_AGE = 60 * 60 * 8; // 8 hours

export const POST: RequestHandler = async ({ request, cookies }) => {
  const { email, password } = await request.json();

  if (!email || !password) {
    return json({ error: 'Email and password required' }, { status: 400 });
  }

  let res: Response;
  try {
    res = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
  } catch {
    return json({ error: 'Cannot reach backend' }, { status: 502 });
  }

  if (!res.ok) {
    const text = await res.text().catch(() => 'Login failed');
    return json({ error: text }, { status: res.status });
  }

  const { token } = (await res.json()) as { token: string };

  cookies.set('sa_jwt', token, {
    path: '/',
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: COOKIE_MAX_AGE
  });

  return json({ ok: true });
};

export const DELETE: RequestHandler = async ({ cookies }) => {
  cookies.delete('sa_jwt', { path: '/' });
  return json({ ok: true });
};
