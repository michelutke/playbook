import { createApiClient } from '$lib/api.js';
import { fail } from '@sveltejs/kit';
import type { PageServerLoad, Actions } from './$types.js';

export const load: PageServerLoad = async ({ locals, url }) => {
  const api = createApiClient(locals.token);
  const status = url.searchParams.get('status') ?? '';
  const search = url.searchParams.get('search') ?? '';

  const clubs = await api.clubs.list({ status: status || undefined, search: search || undefined })
    .catch(() => []);

  return { clubs, status, search };
};

export const actions: Actions = {
  create: async ({ locals, request }) => {
    const api = createApiClient(locals.token);
    const data = await request.formData();
    const name = data.get('name') as string;
    const sportType = data.get('sportType') as string;
    const location = data.get('location') as string;

    if (!name?.trim()) return fail(400, { error: 'Name is required' });
    if (!sportType?.trim()) return fail(400, { error: 'Sport type is required' });

    try {
      await api.clubs.create({ name: name.trim(), sportType: sportType.trim(), location: location?.trim() || undefined });
      return { success: true };
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Failed to create club';
      return fail(500, { error: msg });
    }
  }
};
