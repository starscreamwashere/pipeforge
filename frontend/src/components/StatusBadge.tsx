import { executionStatusColor, workerStatusColor } from '@/lib/format'
import type { ExecutionStatus, PipelineStatus, WorkerStatus } from '@/lib/types'

const PIPELINE_COLORS: Record<PipelineStatus, string> = {
  DRAFT: 'var(--neutral)',
  ACTIVE: 'var(--state-success)',
  PAUSED: 'var(--state-retrying)',
  ARCHIVED: 'var(--state-cancelled)',
}

function Pill({ label, color }: { label: string; color: string }) {
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium"
      style={{ color, backgroundColor: `color-mix(in srgb, ${color} 15%, transparent)` }}
    >
      <span className="h-1.5 w-1.5 rounded-full" style={{ backgroundColor: color }} />
      {label}
    </span>
  )
}

export function ExecutionStatusBadge({ status }: { status: ExecutionStatus }) {
  return <Pill label={status.replace('_', ' ')} color={executionStatusColor(status)} />
}

export function PipelineStatusBadge({ status }: { status: PipelineStatus }) {
  return <Pill label={status} color={PIPELINE_COLORS[status]} />
}

export function WorkerStatusBadge({ status }: { status: WorkerStatus }) {
  return <Pill label={status} color={workerStatusColor(status)} />
}
