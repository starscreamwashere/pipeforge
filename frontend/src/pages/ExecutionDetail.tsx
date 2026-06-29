import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Ban } from 'lucide-react'
import { toast } from 'sonner'
import { dagApi, executionsApi } from '@/lib/services'
import { apiErrorMessage } from '@/lib/api'
import { formatDateTime, formatDuration } from '@/lib/format'
import type { ExecutionStatus } from '@/lib/types'
import { DagView } from '@/components/DagView'
import { ExecutionStatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const TERMINAL: ExecutionStatus[] = ['SUCCESS', 'CANCELLED', 'FAILED_PERMANENTLY']

export default function ExecutionDetail() {
  const { id = '' } = useParams()
  const qc = useQueryClient()

  const detail = useQuery({
    queryKey: ['execution', id],
    queryFn: () => executionsApi.get(id),
    refetchInterval: (query) => {
      const status = query.state.data?.execution.status
      return status && TERMINAL.includes(status) ? false : 3000
    },
  })

  const execution = detail.data?.execution
  const taskRuns = detail.data?.taskRuns ?? []

  const dag = useQuery({
    queryKey: ['dag', execution?.pipelineId],
    queryFn: () => dagApi.getDag(execution!.pipelineId),
    enabled: !!execution?.pipelineId,
  })

  const cancel = useMutation({
    mutationFn: () => executionsApi.cancel(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['execution', id] })
      toast.success('Execution cancelled')
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const statusByTaskId: Record<string, ExecutionStatus> = {}
  taskRuns.forEach((tr) => {
    statusByTaskId[tr.taskId] = tr.status
  })

  const canCancel = execution && !TERMINAL.includes(execution.status)

  return (
    <div className="space-y-6">
      <Link to="/executions" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> Executions
      </Link>

      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="font-mono text-2xl font-bold tracking-tight">{id.slice(0, 12)}</h1>
            {execution && <ExecutionStatusBadge status={execution.status} />}
          </div>
          <Link to={`/pipelines/${execution?.pipelineId}`} className="text-sm text-primary hover:underline">
            View pipeline
          </Link>
        </div>
        {canCancel && (
          <Button variant="destructive" onClick={() => cancel.mutate()} disabled={cancel.isPending}>
            <Ban className="mr-1 h-4 w-4" /> Cancel
          </Button>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Meta label="Trigger" value={execution?.triggerType} />
        <Meta label="Started" value={formatDateTime(execution?.startedAt)} />
        <Meta label="Completed" value={formatDateTime(execution?.completedAt)} />
        <Meta label="Duration" value={formatDuration(execution?.startedAt ?? null, execution?.completedAt ?? null)} />
      </div>

      {execution?.errorMessage && (
        <Card className="border-destructive/40">
          <CardContent className="p-4 text-sm" style={{ color: 'var(--state-failed)' }}>
            {execution.errorMessage}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Task DAG</CardTitle>
        </CardHeader>
        <CardContent className="h-[320px] p-0">
          <DagView
            tasks={dag.data?.tasks ?? []}
            dependencies={dag.data?.dependencies ?? []}
            statusByTaskId={statusByTaskId}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Task runs</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Task</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Attempt</TableHead>
                <TableHead>Worker</TableHead>
                <TableHead>Duration</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {taskRuns.map((tr) => (
                <TableRow key={tr.id}>
                  <TableCell className="font-medium">{tr.taskName}</TableCell>
                  <TableCell>
                    <ExecutionStatusBadge status={tr.status} />
                  </TableCell>
                  <TableCell className="text-muted-foreground">{tr.attempt}</TableCell>
                  <TableCell className="font-mono text-xs text-muted-foreground">
                    {tr.workerId ? tr.workerId.slice(0, 14) : '—'}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {formatDuration(tr.startedAt, tr.completedAt)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}

function Meta({ label, value }: { label: string; value?: string }) {
  return (
    <div className="rounded-lg border border-border p-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-0.5 text-sm font-medium">{value ?? '—'}</div>
    </div>
  )
}
