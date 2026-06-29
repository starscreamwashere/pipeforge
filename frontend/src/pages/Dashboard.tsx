import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Activity, CheckCircle2, GitBranch, XCircle } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { executionsApi, pipelinesApi } from '@/lib/services'
import { executionStatusColor, formatRelative } from '@/lib/format'
import type { ExecutionStatus } from '@/lib/types'
import { KpiCard } from '@/components/KpiCard'
import { ExecutionStatusBadge } from '@/components/StatusBadge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'

const STATUS_ORDER: ExecutionStatus[] = [
  'PENDING', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'RETRYING', 'CANCELLED', 'FAILED_PERMANENTLY',
]

export default function Dashboard() {
  const pipelines = useQuery({ queryKey: ['pipelines', 0], queryFn: () => pipelinesApi.list(0, 1) })
  const executions = useQuery({
    queryKey: ['executions', 'dashboard'],
    queryFn: () => executionsApi.list(0, 100),
    refetchInterval: 5000,
  })
  const runs = executions.data?.content ?? []
  const running = runs.filter((r) => r.status === 'RUNNING' || r.status === 'QUEUED').length
  const failed = runs.filter((r) => r.status === 'FAILED' || r.status === 'FAILED_PERMANENTLY').length
  const succeeded = runs.filter((r) => r.status === 'SUCCESS').length
  const terminal = succeeded + failed
  const successRate = terminal > 0 ? Math.round((succeeded / terminal) * 100) : 0

  const statusData = STATUS_ORDER.map((status) => ({
    status: status.replace('_PERMANENTLY', '_PERM'),
    raw: status,
    count: runs.filter((r) => r.status === status).length,
  })).filter((d) => d.count > 0)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">Operational overview of your pipelines and executions.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          label="Total pipelines"
          value={pipelines.isLoading ? '—' : (pipelines.data?.totalElements ?? 0)}
          icon={<GitBranch className="h-5 w-5" />}
        />
        <KpiCard
          label="Active executions"
          value={running}
          accent="var(--state-running)"
          icon={<Activity className="h-5 w-5" />}
        />
        <KpiCard
          label="Failed executions"
          value={failed}
          accent={failed > 0 ? 'var(--state-failed)' : undefined}
          icon={<XCircle className="h-5 w-5" />}
        />
        <KpiCard
          label="Success rate"
          value={`${successRate}%`}
          accent="var(--state-success)"
          icon={<CheckCircle2 className="h-5 w-5" />}
        />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Executions by status</CardTitle>
          </CardHeader>
          <CardContent className="h-[280px]">
            {executions.isLoading ? (
              <Skeleton className="h-full w-full" />
            ) : statusData.length === 0 ? (
              <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                No executions yet.
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={statusData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                  <XAxis dataKey="status" stroke="var(--muted-foreground)" fontSize={11} />
                  <YAxis stroke="var(--muted-foreground)" fontSize={11} allowDecimals={false} />
                  <Tooltip
                    contentStyle={{ background: 'var(--popover)', border: '1px solid var(--border)', borderRadius: 8 }}
                  />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                    {statusData.map((d) => (
                      <Cell key={d.raw} fill={executionStatusColor(d.raw)} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Recent executions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {executions.isLoading ? (
              Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)
            ) : runs.length === 0 ? (
              <div className="py-8 text-center text-sm text-muted-foreground">Trigger a pipeline run to see it here.</div>
            ) : (
              runs.slice(0, 6).map((run) => (
                <Link
                  key={run.id}
                  to={`/executions/${run.id}`}
                  className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm hover:bg-accent"
                >
                  <span className="font-mono text-xs text-muted-foreground">{run.id.slice(0, 8)}</span>
                  <span className="text-muted-foreground">{run.triggerType}</span>
                  <span className="text-muted-foreground">{formatRelative(run.createdAt)}</span>
                  <ExecutionStatusBadge status={run.status} />
                </Link>
              ))
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
