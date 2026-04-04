import { getSession, getToken } from '$lib/server/auth';
import type { Handle } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
	const user = await getSession(event.cookies);
	event.locals.user = user ?? undefined;
	event.locals.token = getToken(event.cookies) ?? undefined;
	return resolve(event);
};
