import { apiGet, apiPost, apiPatch, apiDelete } from '$lib/server/api';
import { fail, redirect } from '@sveltejs/kit';
import type { Actions, PageServerLoad } from './$types';

interface Manager {
	id: string;
	displayName: string;
	email: string;
}

interface ClubDetail {
	id: string;
	name: string;
	sportType: string;
	location: string | null;
	status: string;
	createdAt: string;
	managers: Manager[];
}

export const load: PageServerLoad = async ({ params, locals }) => {
	const club = await apiGet<ClubDetail>(`/admin/clubs/${params.clubId}`, locals.token!);
	return { club };
};

export const actions: Actions = {
	edit: async ({ request, params, locals }) => {
		const data = await request.formData();
		const name = (data.get('name') as string) || undefined;
		const location = (data.get('location') as string) || undefined;
		const sportType = (data.get('sportType') as string) || undefined;
		await apiPatch(`/admin/clubs/${params.clubId}`, locals.token!, { name, location, sportType });
		return { success: true };
	},

	deactivate: async ({ params, locals }) => {
		await apiPost(`/admin/clubs/${params.clubId}/deactivate`, locals.token!);
		return { success: true, action: 'deactivated' };
	},

	reactivate: async ({ params, locals }) => {
		await apiPost(`/admin/clubs/${params.clubId}/reactivate`, locals.token!);
		return { success: true, action: 'reactivated' };
	},

	delete: async ({ params, locals }) => {
		await apiDelete(`/admin/clubs/${params.clubId}`, locals.token!);
		throw redirect(302, '/admin/clubs');
	},

	addManager: async ({ request, params, locals }) => {
		const data = await request.formData();
		const email = data.get('email') as string;
		if (!email) return fail(400, { error: 'Email required' });
		try {
			await apiPost(`/admin/clubs/${params.clubId}/managers`, locals.token!, { email });
			return { success: true, action: 'manager_added' };
		} catch {
			return fail(404, { error: 'User not found' });
		}
	},

	removeManager: async ({ request, params, locals }) => {
		const data = await request.formData();
		const userId = data.get('userId') as string;
		await apiDelete(`/admin/clubs/${params.clubId}/managers/${userId}`, locals.token!);
		return { success: true, action: 'manager_removed' };
	}
};
