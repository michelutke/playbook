import { createApiClient, ApiError } from '$lib/api.js';
import { fail, error } from '@sveltejs/kit';
import type { PageServerLoad, Actions } from './$types.js';

export const load: PageServerLoad = async ({ locals, params }) => {
  const api = createApiClient(locals.token);

  const [club, managers] = await Promise.all([
    api.clubs.get(params.id).catch((e) => {
      if (e instanceof ApiError && e.status === 404) throw error(404, 'Club not found');
      throw error(500, 'Failed to load club');
    }),
    api.clubs.managers.list(params.id).catch(() => [])
  ]);

  return { club, managers };
};

export const actions: Actions = {
  update: async ({ locals, params, request }) => {
    const api = createApiClient(locals.token);
    const data = await request.formData();
    const name = data.get('name') as string;
    const sportType = data.get('sportType') as string;
    const location = data.get('location') as string;

    if (!name?.trim()) return fail(400, { error: 'Name is required', action: 'update' });

    try {
      await api.clubs.update(params.id, {
        name: name.trim(),
        sportType: sportType?.trim() || undefined,
        location: location?.trim() || undefined
      });
      return { success: true, action: 'update' };
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Update failed';
      return fail(500, { error: msg, action: 'update' });
    }
  },

  inviteManager: async ({ locals, params, request }) => {
    const api = createApiClient(locals.token);
    const data = await request.formData();
    const email = data.get('email') as string;

    if (!email?.trim()) return fail(400, { error: 'Email is required', action: 'invite' });

    try {
      await api.clubs.managers.invite(params.id, email.trim());
      return { success: true, action: 'invite' };
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Invite failed';
      return fail(500, { error: msg, action: 'invite' });
    }
  },

  removeManager: async ({ locals, params, request }) => {
    const api = createApiClient(locals.token);
    const data = await request.formData();
    const managerId = data.get('managerId') as string;

    try {
      await api.clubs.managers.remove(params.id, managerId);
      return { success: true, action: 'removeManager' };
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Remove failed';
      return fail(500, { error: msg, action: 'removeManager' });
    }
  },

  deactivate: async ({ locals, params }) => {
    const api = createApiClient(locals.token);
    try {
      await api.clubs.deactivate(params.id);
      return { success: true, action: 'deactivate' };
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Deactivate failed';
      return fail(500, { error: msg, action: 'deactivate' });
    }
  },

  reactivate: async ({ locals, params }) => {
    const api = createApiClient(locals.token);
    try {
      await api.clubs.reactivate(params.id);
      return { success: true, action: 'reactivate' };
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Reactivate failed';
      return fail(500, { error: msg, action: 'reactivate' });
    }
  },

  delete: async ({ locals, params, request }) => {
    const api = createApiClient(locals.token);
    const data = await request.formData();
    const confirmName = data.get('confirmName') as string;
    const club = await api.clubs.get(params.id).catch(() => null);

    if (!club || confirmName !== club.name) {
      return fail(400, { error: 'Club name does not match', action: 'delete' });
    }

    try {
      await api.clubs.delete(params.id);
      return { success: true, action: 'delete', redirect: '/clubs' };
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Delete failed';
      return fail(500, { error: msg, action: 'delete' });
    }
  },

  impersonate: async ({ locals, params, request }) => {
    const api = createApiClient(locals.token);
    const data = await request.formData();
    const managerId = data.get('managerId') as string;
    const managerEmail = data.get('managerEmail') as string;

    try {
      const result = await api.clubs.managers.impersonate(params.id, managerId);
      return {
        success: true,
        action: 'impersonate',
        impersonation: {
          ...result,
          managerId,
          managerEmail,
          clubId: params.id
        }
      };
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Impersonation failed';
      return fail(500, { error: msg, action: 'impersonate' });
    }
  }
};
