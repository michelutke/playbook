import { apiGet } from '$lib/server/api';
import type { PageServerLoad } from './$types';

interface AdminStats {
	totalClubs: number;
	totalUsers: number;
	activeEventsToday: number;
	recentSignUps: number;
	recentUsers: Array<{
		id: string;
		displayName: string;
		email: string;
		joinedAt: string;
	}>;
}

export const load: PageServerLoad = async ({ locals }) => {
	const stats = await apiGet<AdminStats>('/admin/stats', locals.token!);
	return { stats };
};
