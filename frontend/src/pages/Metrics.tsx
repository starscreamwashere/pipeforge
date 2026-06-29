import { useQuery } from '@tanstack/react-query'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { executionsApi, workersApi } from '@/lib/services'
import { KpiCard } from '@/components/KpiCard'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function Metrics() {
  const executions = useQuery({
    queryKey: ['executions', 'metrics'],
    queryFn: () => executionsApi.list(0, 100),
    refetchInterval: 5000,
  })
  const workers = useQuery({ queryKey: ['workers'], queryFn: workersApi.list, refetchInterval: 10_000 })

  const runs = executions.data?.content ?? []
  const succeeded = runs.filter((r) => r.status === 'SUCCESS').length
  const failed = runs.filter((r) => r.status === 'FAILED' || r.status === 'FAILED_PERMANENTLY').length
  const retrying = runs.filter((r) => r.status === 'RETRYING').length
  const terminal = succeeded + failed
  const failureRate = terminal > 0 ? Math.round((failed / terminal) * 100) : 0
  const totalJobs = (workers.data ?? []).reduce((sum, w) => sum + w.jobsProcessed, 0)

  // Throughput: executions bucketed per minute (last 10 buckets present in the data).
  const buckets = new Map<string, number>()
  runs.forEach((r) => {
    const d = new Date(r.createdAt)
    const key = `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
    buckets.set(key, (buckets.get(key) ?? 0) + 1)
  })
  const throughput = Array.from(buckets.entries())
    .map(([time, count]) => ({ time, count }))
    .slice(-12)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Metrics</h1>
        <p className="text-muted-foreground">Performance and throughput observability.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard label="Jobs processed" value={totalJobs} />
        <KpiCard label="Succeeded" value={succeeded} accent="var(--state-success)" />
        <KpiCard label="Failure rate" value={`${failureRate}%`} accent={failed > 0 ? 'var(--state-failed)' : undefined} />
        <KpiCard label="Retrying" value={retrying} accent="var(--state-retrying)" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Execution throughput</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px]">
          {throughput.length === 0 ? (
            <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
              No execution data yet.
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={throughput}>
                <defs>
                  <linearGradient id="tp" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="var(--primary)" stopOpacity={0.4} />
                    <stop offset="100%" stopColor="var(--primary)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis dataKey="time" stroke="var(--muted-foreground)" fontSize={11} />
                <YAxis stroke="var(--muted-foreground)" fontSize={11} allowDecimals={false} />
                <Tooltip
                  contentStyle={{ background: 'var(--popover)', border: '1px solid var(--border)', borderRadius: 8 }}
                />
                <Area type="monotone" dataKey="count" stroke="var(--primary)" fill="url(#tp)" />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
