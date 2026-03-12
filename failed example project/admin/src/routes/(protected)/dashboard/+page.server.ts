import { createApiClient } from '$lib/api.js';
import type { PageServerLoad } from './$types.js';

export const load: PageServerLoad = async ({ locals }) => {
  const api = createApiClient(locals.token);

  const [stats, auditLog] = await Promise.all([
    api.stats.get().catch(() => null),
    api.auditLog.list({ pageSize: 10, page: 0 }).catch(() => null)
  ]);

  return { stats, auditLog };
};
