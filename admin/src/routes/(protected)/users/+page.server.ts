import { createApiClient } from '$lib/api.js';
import type { PageServerLoad } from './$types.js';

export const load: PageServerLoad = async ({ locals, url }) => {
  const q = url.searchParams.get('q') ?? '';
  if (!q.trim()) return { users: [], q };

  const api = createApiClient(locals.token);
  const users = await api.users.search(q).catch(() => []);
  return { users, q };
};
