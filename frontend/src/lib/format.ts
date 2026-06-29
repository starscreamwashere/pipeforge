import type { ExecutionStatus, WorkerStatus } from './types'

/** Maps an execution status to its theme CSS variable (UI/UX brief §4 — globally consistent). */
export function executionStatusColor(status: ExecutionStatus): string {
  switch (status) {
    case 'PENDING':
      return 'var(--state-pending)'
    case 'QUEUED':
      return 'var(--state-queued)'
    case 'RUNNING':
      return 'var(--state-running)'
    case 'SUCCESS':
      return 'var(--state-success)'
    case 'FAILED':
    case 'FAILED_PERMANENTLY':
      return 'var(--state-failed)'
    case 'RETRYING':
      return 'var(--state-retrying)'
    case 'CANCELLED':
      return 'var(--state-cancelled)'
    default:
      return 'var(--neutral)'
  }
}

export function workerStatusColor(status: WorkerStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'var(--state-success)'
    case 'BUSY':
      return 'var(--state-running)'
    case 'OFFLINE':
      return 'var(--state-cancelled)'
    default:
      return 'var(--neutral)'
  }
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

export function formatRelative(iso: string | null | undefined): string {
  if (!iso) return '—'
  const diffMs = Date.now() - new Date(iso).getTime()
  const sec = Math.round(diffMs / 1000)
  if (sec < 60) return `${sec}s ago`
  const min = Math.round(sec / 60)
  if (min < 60) return `${min}m ago`
  const hr = Math.round(min / 60)
  if (hr < 24) return `${hr}h ago`
  return `${Math.round(hr / 24)}d ago`
}

/** Duration between two ISO timestamps, or from start to now if not complete. */
export function formatDuration(start: string | null, end: string | null): string {
  if (!start) return '—'
  const ms = (end ? new Date(end).getTime() : Date.now()) - new Date(start).getTime()
  if (ms < 0) return '—'
  const sec = Math.floor(ms / 1000)
  if (sec < 60) return `${sec}s`
  const min = Math.floor(sec / 60)
  return `${min}m ${sec % 60}s`
}
