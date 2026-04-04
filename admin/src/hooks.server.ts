import { getSession, getToken } from '$lib/server/auth';
import { getImpersonationState, endImpersonation } from '$lib/server/impersonation';
import type { Handle } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
	const user = await getSession(event.cookies);
	event.locals.user = user ?? undefined;
	event.locals.token = getToken(event.cookies) ?? undefined;

	const impersonation = getImpersonationState(event.cookies);
	if (impersonation.active && impersonation.expiresAt && Date.now() > impersonation.expiresAt) {
		await endImpersonation(event.cookies);
		// Re-get session with original token
		const refreshedUser = await getSession(event.cookies);
		event.locals.user = refreshedUser ?? undefined;
		event.locals.token = getToken(event.cookies) ?? undefined;
		event.locals.impersonation = undefined;
	} else {
		event.locals.impersonation = impersonation.active ? impersonation : undefined;
	}

	return resolve(event);
};
