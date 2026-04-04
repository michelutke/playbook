const API_BASE = process.env.API_URL || 'http://localhost:8080';

export async function apiGet<T = unknown>(path: string, token: string): Promise<T> {
	const res = await fetch(`${API_BASE}${path}`, {
		headers: { Authorization: `Bearer ${token}` }
	});
	if (!res.ok) {
		throw new Error(`API GET ${path} failed: ${res.status}`);
	}
	return res.json() as Promise<T>;
}

export async function apiPost<T = unknown>(path: string, token: string, body: unknown): Promise<T> {
	const res = await fetch(`${API_BASE}${path}`, {
		method: 'POST',
		headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
		body: JSON.stringify(body)
	});
	if (!res.ok) {
		throw new Error(`API POST ${path} failed: ${res.status}`);
	}
	return res.json() as Promise<T>;
}

export async function apiPatch<T = unknown>(
	path: string,
	token: string,
	body: unknown
): Promise<T> {
	const res = await fetch(`${API_BASE}${path}`, {
		method: 'PATCH',
		headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
		body: JSON.stringify(body)
	});
	if (!res.ok) {
		throw new Error(`API PATCH ${path} failed: ${res.status}`);
	}
	return res.json() as Promise<T>;
}

export async function apiDelete(path: string, token: string): Promise<void> {
	const res = await fetch(`${API_BASE}${path}`, {
		method: 'DELETE',
		headers: { Authorization: `Bearer ${token}` }
	});
	if (!res.ok) {
		throw new Error(`API DELETE ${path} failed: ${res.status}`);
	}
}
