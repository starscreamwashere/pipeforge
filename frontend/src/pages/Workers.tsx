import { useQuery } from '@tanstack/react-query'
import { Cpu } from 'lucide-react'
import { workersApi } from '@/lib/services'
import { formatRelative } from '@/lib/format'
import { WorkerStatusBadge } from '@/components/StatusBadge'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'

export default function Workers() {
  const { data, isLoading } = useQuery({
    queryKey: ['workers'],
    queryFn: workersApi.list,
    refetchInterval: 5000,
  })
  const workers = data ?? []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Workers</h1>
        <p className="text-muted-foreground">Monitor the worker nodes consuming and executing jobs.</p>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-36 w-full" />
          ))}
        </div>
      ) : workers.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No workers registered. Start the app with{' '}
            <code className="font-mono text-foreground">WORKER_CONSUMER_ENABLED=true</code> to run the embedded worker.
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {workers.map((w) => (
            <Card key={w.id}>
              <CardContent className="space-y-3 p-5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Cpu className="h-4 w-4 text-muted-foreground" />
                    <span className="font-mono text-xs">{w.name.slice(0, 20)}</span>
                  </div>
                  <WorkerStatusBadge status={w.status} />
                </div>
                <div className="grid grid-cols-2 gap-2 text-sm">
                  <div>
                    <div className="text-xs text-muted-foreground">Jobs processed</div>
                    <div className="text-lg font-semibold">{w.jobsProcessed}</div>
                  </div>
                  <div>
                    <div className="text-xs text-muted-foreground">Heartbeat</div>
                    <div className="text-lg font-semibold">{formatRelative(w.lastHeartbeatAt)}</div>
                  </div>
                </div>
                <div className="text-xs text-muted-foreground">
                  Current job: {w.currentTaskRunId ? w.currentTaskRunId.slice(0, 8) : 'idle'}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
