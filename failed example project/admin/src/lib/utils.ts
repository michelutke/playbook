import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('en-CH', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(iso));
}

export function formatDateOnly(iso: string): string {
  return new Intl.DateTimeFormat('en-CH', { dateStyle: 'medium' }).format(new Date(iso));
}

export function statusColor(status: string): string {
  switch (status.toLowerCase()) {
    case 'active':
      return 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30';
    case 'inactive':
    case 'deactivated':
      return 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30';
    case 'pending':
      return 'bg-amber-500/20 text-amber-400 border-amber-500/30';
    case 'rejected':
      return 'bg-red-500/20 text-red-400 border-red-500/30';
    default:
      return 'bg-zinc-700/50 text-zinc-300 border-zinc-600/30';
  }
}

export function truncate(str: string, n: number): string {
  return str.length > n ? str.slice(0, n) + '…' : str;
}
