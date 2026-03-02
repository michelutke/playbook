import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types.js';

const API_URL = process.env.PUBLIC_API_URL ?? 'http://localhost:8080';

export const POST: RequestHandler = async ({ cookies }) => {
  const token = cookies.get('sa_jwt');
  if (!token) return json({ error: 'Unauthorized' }, { status: 401 });

  const res = await fetch(`${API_URL}/sa/audit-log/export`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` }
  });

  if (!res.ok) {
    const text = await res.text().catch(() => 'Export failed');
    return json({ error: text }, { status: res.status });
  }

  return json(await res.json());
};
