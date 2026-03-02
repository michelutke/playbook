import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types.js';

const API_URL = process.env.PUBLIC_API_URL ?? 'http://localhost:8080';

export const GET: RequestHandler = async ({ cookies, params }) => {
  const token = cookies.get('sa_jwt');
  if (!token) return json({ error: 'Unauthorized' }, { status: 401 });

  const res = await fetch(`${API_URL}/sa/audit-log/export/${params.jobId}`, {
    headers: { Authorization: `Bearer ${token}` }
  });

  if (!res.ok) {
    const text = await res.text().catch(() => 'Status check failed');
    return json({ error: text }, { status: res.status });
  }

  return json(await res.json());
};
