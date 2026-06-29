import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { executionsApi } from '@/lib/services'
import { formatDuration, formatRelative } from '@/lib/format'
import { ExecutionStatusBadge } from '@/components/StatusBadge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'

export default function Executions() {
  const navigate = useNavigate()
  const { data, isLoading } = useQuery({
    queryKey: ['executions', 'all'],
    queryFn: () => executionsApi.list(0, 50),
    refetchInterval: 5000,
  })
  const runs = data?.content ?? []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Executions</h1>
        <p className="text-muted-foreground">Track all pipeline runs across the system.</p>
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Execution</TableHead>
              <TableHead>Trigger</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Started</TableHead>
              <TableHead>Duration</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={5}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : runs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="py-12 text-center text-muted-foreground">
                  No executions yet — trigger a pipeline run.
                </TableCell>
              </TableRow>
            ) : (
              runs.map((run) => (
                <TableRow
                  key={run.id}
                  className="cursor-pointer"
                  onClick={() => navigate(`/executions/${run.id}`)}
                >
                  <TableCell className="font-mono text-xs">{run.id.slice(0, 12)}</TableCell>
                  <TableCell className="text-muted-foreground">{run.triggerType}</TableCell>
                  <TableCell>
                    <ExecutionStatusBadge status={run.status} />
                  </TableCell>
                  <TableCell className="text-muted-foreground">{formatRelative(run.startedAt ?? run.createdAt)}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {formatDuration(run.startedAt, run.completedAt)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
