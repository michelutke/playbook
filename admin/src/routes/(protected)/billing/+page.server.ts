import { createApiClient } from '$lib/api.js';
import type { PageServerLoad } from './$types.js';

export const load: PageServerLoad = async ({ locals }) => {
  const api = createApiClient(locals.token);
  const entries = await api.billing.summary().catch(() => []);
  return { entries };
};
