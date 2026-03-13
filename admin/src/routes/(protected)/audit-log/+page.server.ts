import { createApiClient } from '$lib/api.js';
import type { PageServerLoad } from './$types.js';

export const load: PageServerLoad = async ({ locals, url }) => {
  const api = createApiClient(locals.token);

  const actorId = url.searchParams.get('actorId') ?? '';
  const action = url.searchParams.get('action') ?? '';
  const from = url.searchParams.get('from') ?? '';
  const to = url.searchParams.get('to') ?? '';
  const page = parseInt(url.searchParams.get('page') ?? '0', 10);

  const result = await api.auditLog.list({
    actorId: actorId || undefined,
    action: action || undefined,
    from: from || undefined,
    to: to || undefined,
    page,
    pageSize: 50
  }).catch(() => ({ items: [], total: 0, page: 0, pageSize: 50 }));

  return { result, filters: { actorId, action, from, to }, page };
};
