import type { AuditLogEntry } from './types.js'

const HEADERS = ['id', 'actorId', 'action', 'targetType', 'targetId', 'payload', 'impersonatedAs', 'impersonationSessionId', 'createdAt']

function escapeCsv(value: string | null | undefined): string {
  if (value == null) return ''
  const str = String(value)
  // Wrap in quotes if it contains comma, double-quote, or newline
  if (str.includes(',') || str.includes('"') || str.includes('\n')) {
    return `"${str.replace(/"/g, '""')}"`
  }
  return str
}

export function toCsv(entries: AuditLogEntry[]): string {
  const rows: string[] = [HEADERS.join(',')]
  for (const e of entries) {
    rows.push([
      escapeCsv(e.id),
      escapeCsv(e.actorId),
      escapeCsv(e.action),
      escapeCsv(e.targetType),
      escapeCsv(e.targetId),
      escapeCsv(e.payload),
      escapeCsv(e.impersonatedAs),
      escapeCsv(e.impersonationSessionId),
      escapeCsv(e.createdAt),
    ].join(','))
  }
  return rows.join('\n')
}
