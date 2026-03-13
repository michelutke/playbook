import type { LayoutServerLoad } from './$types.js';

export const load: LayoutServerLoad = async ({ locals, url }) => {
  // Public paths handled by hooks
  if (!locals.token) {
    return { token: null, userId: null };
  }

  return {
    token: locals.token,
    userId: locals.userId
  };
};
