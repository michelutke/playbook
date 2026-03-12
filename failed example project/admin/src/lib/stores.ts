import type { ImpersonationResponse } from './types.js';

export interface ImpersonationState {
  token: string;
  sessionId: string;
  expiresAt: string;
  managerId: string;
  managerEmail: string;
  clubId: string;
}

const STORAGE_KEY = 'sa_impersonation';

export function loadImpersonation(): ImpersonationState | null {
  if (typeof sessionStorage === 'undefined') return null;
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as ImpersonationState;
  } catch {
    return null;
  }
}

export function saveImpersonation(state: ImpersonationState): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

export function clearImpersonation(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}

export function formatCountdown(expiresAt: string): string {
  const diff = Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000));
  const mm = Math.floor(diff / 60).toString().padStart(2, '0');
  const ss = (diff % 60).toString().padStart(2, '0');
  return `${mm}:${ss}`;
}

export function isExpired(expiresAt: string): boolean {
  return new Date(expiresAt).getTime() <= Date.now();
}

export function secondsRemaining(expiresAt: string): number {
  return Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000));
}
