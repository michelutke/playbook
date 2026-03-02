import type { Handle } from '@sveltejs/kit';

const PUBLIC_PATHS = ['/login', '/api/login'];

export const handle: Handle = async ({ event, resolve }) => {
  const path = event.url.pathname;

  // Skip auth check for public paths
  if (PUBLIC_PATHS.some((p) => path.startsWith(p))) {
    return resolve(event);
  }

  const token = event.cookies.get('sa_jwt');

  if (!token) {
    return Response.redirect(`${event.url.origin}/login`, 302);
  }

  // Decode JWT payload (no verification — Ktor validates on every API call)
  try {
    const [, payloadB64] = token.split('.');
    const payload = JSON.parse(atob(payloadB64.replace(/-/g, '+').replace(/_/g, '/')));
    event.locals.token = token;
    event.locals.userId = payload.sub as string;
  } catch {
    event.cookies.delete('sa_jwt', { path: '/' });
    return Response.redirect(`${event.url.origin}/login`, 302);
  }

  return resolve(event);
};
