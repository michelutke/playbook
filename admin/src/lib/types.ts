export interface SaStats {
  totalClubs: number;
  totalUsers: number;
  activeEventsToday: number;
  signUpsLast7Days: number;
}

export interface SaClub {
  id: string;
  name: string;
  status: string;
  sportType: string;
  location: string | null;
  metadata: string | null;
  createdAt: string;
  managerCount: number;
  memberCount: number;
  teamCount: number;
}

export interface SaManager {
  id: string;
  clubId: string;
  userId: string | null;
  invitedEmail: string;
  displayName: string | null;
  status: string;
  addedAt: string;
  acceptedAt: string | null;
}

export interface AuditLogEntry {
  id: string;
  actorId: string;
  action: string;
  targetType: string | null;
  targetId: string | null;
  payload: string | null;
  impersonatedAs: string | null;
  impersonationSessionId: string | null;
  createdAt: string;
}

export interface ClubBillingEntry {
  clubId: string;
  clubName: string;
  activeMemberCount: number;
  annualBillingChf: number;
}

export interface ImpersonationResponse {
  token: string;
  sessionId: string;
  expiresAt: string;
}

export interface PagedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface ExportJob {
  jobId: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  downloadUrl?: string;
}

export interface UserSearchResult {
  id: string;
  email: string;
  displayName: string | null;
  createdAt: string;
  clubMemberships: { clubId: string; clubName: string; role: string }[];
}
