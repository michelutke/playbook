import { redirect } from '@sveltejs/kit';
import type { LayoutServerLoad } from './$types.js';

export const load: LayoutServerLoad = async ({ locals }) => {
  if (!locals.token) {
    throw redirect(302, '/login');
  }
  return {
    token: locals.token,
    userId: locals.userId
  };
};
