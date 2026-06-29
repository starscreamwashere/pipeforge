import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { executionsApi } from '@/lib/services'
import { formatDateTime } from '@/lib/format'
import type { Execution } from '@/lib/types'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

type Severity = 'INFO' | 'WARN' | 'ERROR'

function severityOf(e: Execution): Severity {
  if (e.status === 'FAILED' || e.status === 'FAILED_PERMANENTLY') return 'ERROR'
  if (e.status === 'RETRYING' || e.status === 'CANCELLED') return 'WARN'
  return 'INFO'
}

const SEVERITY_COLOR: Record<Severity, string> = {
  INFO: 'var(--info)',
  WARN: 'var(--warning)',
  ERROR: 'var(--error)',
}

export default function Logs() {
  const { data } = useQuery({
    queryKey: ['executions', 'logs'],
    queryFn: () => executionsApi.list(0, 100),
    refetchInterval: 5000,
  })
  const [search, setSearch] = useState('')
  const [severity, setSeverity] = useState<string>('ALL')

  const entries = useMemo(() => {
    const runs = data?.content ?? []
    return runs
      .map((e) => ({
        time: e.startedAt ?? e.createdAt,
        severity: severityOf(e),
        source: `execution:${e.id.slice(0, 8)}`,
        message: `Pipeline run ${e.status} (trigger: ${e.triggerType})${e.errorMessage ? ` — ${e.errorMessage}` : ''}`,
      }))
      .filter((row) => (severity === 'ALL' ? true : row.severity === severity))
      .filter((row) =>
        search ? (row.message + row.source).toLowerCase().includes(search.toLowerCase()) : true,
      )
  }, [data, search, severity])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Logs</h1>
        <p className="text-muted-foreground">Execution event log. Application logs stream as structured JSON.</p>
      </div>

      <div className="flex gap-3">
        <Input placeholder="Search logs…" value={search} onChange={(e) => setSearch(e.target.value)} className="max-w-sm" />
        <Select value={severity} onValueChange={setSeverity}>
          <SelectTrigger className="w-36">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {['ALL', 'INFO', 'WARN', 'ERROR'].map((s) => (
              <SelectItem key={s} value={s}>
                {s}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Card>
        <CardContent className="max-h-[60vh] overflow-auto p-4 font-mono text-xs">
          {entries.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">No log events.</div>
          ) : (
            entries.map((row, i) => (
              <div key={i} className="flex gap-3 border-b border-border/50 py-1.5">
                <span className="shrink-0 text-muted-foreground">{formatDateTime(row.time)}</span>
                <span className="w-12 shrink-0 font-semibold" style={{ color: SEVERITY_COLOR[row.severity] }}>
                  {row.severity}
                </span>
                <span className="shrink-0 text-muted-foreground">{row.source}</span>
                <span>{row.message}</span>
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </div>
  )
}
