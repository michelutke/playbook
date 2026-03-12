import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types.js';

export const load: PageServerLoad = async ({ locals }) => {
  // If already logged in, go to dashboard
  if (locals.token) {
    throw redirect(302, '/');
  }
  return {};
};
