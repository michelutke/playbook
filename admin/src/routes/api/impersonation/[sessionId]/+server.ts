import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types.js';

const API_URL = process.env.PUBLIC_API_URL ?? 'http://localhost:8080';

export const POST: RequestHandler = async ({ request, params }) => {
  // The impersonation token is sent from the client (stored in sessionStorage)
  const authHeader = request.headers.get('Authorization');
  if (!authHeader) return json({ error: 'Unauthorized' }, { status: 401 });

  const res = await fetch(`${API_URL}/sa/impersonation/${params.sessionId}/end`, {
    method: 'POST',
    headers: { Authorization: authHeader }
  });

  if (!res.ok) {
    const text = await res.text().catch(() => 'End session failed');
    return json({ error: text }, { status: res.status });
  }

  return json({ ok: true });
};
