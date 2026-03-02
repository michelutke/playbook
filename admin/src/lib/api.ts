import type {
  SaStats,
  SaClub,
  SaManager,
  AuditLogEntry,
  ClubBillingEntry,
  ImpersonationResponse,
  PagedResponse,
  ExportJob,
  UserSearchResult
} from './types.js';

const API_URL = process.env.PUBLIC_API_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(
  path: string,
  token: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_URL}${path}`;
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(options.headers ?? {})
    }
  });

  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new ApiError(res.status, text);
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export function createApiClient(token: string) {
  return {
    auth: {
      login: (email: string, password: string) =>
        fetch(`${API_URL}/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        })
    },

    stats: {
      get: () => request<SaStats>('/sa/stats', token)
    },

    auditLog: {
      list: (params: {
        actorId?: string;
        action?: string;
        from?: string;
        to?: string;
        page?: number;
        pageSize?: number;
      }) => {
        const q = new URLSearchParams();
        if (params.actorId) q.set('actorId', params.actorId);
        if (params.action) q.set('action', params.action);
        if (params.from) q.set('from', params.from);
        if (params.to) q.set('to', params.to);
        if (params.page != null) q.set('page', String(params.page));
        if (params.pageSize != null) q.set('pageSize', String(params.pageSize));
        return request<PagedResponse<AuditLogEntry>>(`/sa/audit-log?${q}`, token);
      },
      export: () =>
        request<ExportJob>('/sa/audit-log/export', token, { method: 'POST' }),
      exportStatus: (jobId: string) =>
        request<ExportJob>(`/sa/audit-log/export/${jobId}`, token)
    },

    clubs: {
      list: (params: { status?: string; search?: string } = {}) => {
        const q = new URLSearchParams();
        if (params.status) q.set('status', params.status);
        if (params.search) q.set('search', params.search);
        return request<SaClub[]>(`/sa/clubs?${q}`, token);
      },
      get: (id: string) => request<SaClub>(`/sa/clubs/${id}`, token),
      create: (data: { name: string; sportType: string; location?: string }) =>
        request<SaClub>('/sa/clubs', token, {
          method: 'POST',
          body: JSON.stringify(data)
        }),
      update: (id: string, data: Partial<{ name: string; sportType: string; location: string }>) =>
        request<SaClub>(`/sa/clubs/${id}`, token, {
          method: 'PATCH',
          body: JSON.stringify(data)
        }),
      deactivate: (id: string) =>
        request<SaClub>(`/sa/clubs/${id}/deactivate`, token, { method: 'POST' }),
      reactivate: (id: string) =>
        request<SaClub>(`/sa/clubs/${id}/reactivate`, token, { method: 'POST' }),
      delete: (id: string) =>
        request<void>(`/sa/clubs/${id}`, token, { method: 'DELETE' }),

      managers: {
        list: (clubId: string) =>
          request<SaManager[]>(`/sa/clubs/${clubId}/managers`, token),
        invite: (clubId: string, email: string) =>
          request<SaManager>(`/sa/clubs/${clubId}/managers`, token, {
            method: 'POST',
            body: JSON.stringify({ email })
          }),
        remove: (clubId: string, managerId: string) =>
          request<void>(`/sa/clubs/${clubId}/managers/${managerId}`, token, {
            method: 'DELETE'
          }),
        impersonate: (clubId: string, managerId: string) =>
          request<ImpersonationResponse>(
            `/sa/clubs/${clubId}/managers/${managerId}/impersonate`,
            token,
            { method: 'POST' }
          )
      }
    },

    impersonation: {
      end: (sessionId: string, impersonationToken: string) =>
        request<void>(`/sa/impersonation/${sessionId}/end`, impersonationToken, {
          method: 'POST'
        })
    },

    users: {
      search: (q: string) =>
        request<UserSearchResult[]>(`/sa/users/search?q=${encodeURIComponent(q)}`, token)
    },

    billing: {
      summary: () => request<ClubBillingEntry[]>('/sa/billing/summary', token)
    }
  };
}
