import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Play, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { dagApi, executionsApi, pipelinesApi, type CreateTaskInput } from '@/lib/services'
import { apiErrorMessage } from '@/lib/api'
import type { TaskType } from '@/lib/types'
import { DagView } from '@/components/DagView'
import { PipelineStatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const TASK_TYPES: TaskType[] = ['EXTRACT', 'TRANSFORM', 'LOAD', 'CUSTOM']

export default function PipelineDetail() {
  const { id = '' } = useParams()
  const qc = useQueryClient()
  const navigate = useNavigate()

  const pipeline = useQuery({ queryKey: ['pipeline', id], queryFn: () => pipelinesApi.get(id) })
  const dag = useQuery({ queryKey: ['dag', id], queryFn: () => dagApi.getDag(id) })

  const [taskName, setTaskName] = useState('')
  const [taskType, setTaskType] = useState<TaskType>('EXTRACT')
  const [simulateFail, setSimulateFail] = useState(false)
  const [parent, setParent] = useState('')
  const [child, setChild] = useState('')

  const refresh = () => qc.invalidateQueries({ queryKey: ['dag', id] })

  const trigger = useMutation({
    mutationFn: () => executionsApi.trigger(id),
    onSuccess: (run) => {
      toast.success('Execution triggered')
      navigate(`/executions/${run.id}`)
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const setStatus = useMutation({
    mutationFn: (status: string) => pipelinesApi.update(id, { status }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pipeline', id] })
      toast.success('Pipeline updated')
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: () => pipelinesApi.remove(id),
    onSuccess: () => {
      toast.success('Pipeline deleted')
      navigate('/pipelines')
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const addTask = useMutation({
    mutationFn: () => {
      const input: CreateTaskInput = {
        taskName,
        taskType,
        configPayload: simulateFail ? { fail: true } : null,
      }
      return dagApi.addTask(id, input)
    },
    onSuccess: () => {
      setTaskName('')
      setSimulateFail(false)
      refresh()
      toast.success('Task added')
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const addDependency = useMutation({
    mutationFn: () => dagApi.addDependency(id, parent, child),
    onSuccess: () => {
      setParent('')
      setChild('')
      refresh()
      toast.success('Dependency added')
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const deleteTask = useMutation({
    mutationFn: (taskId: string) => dagApi.deleteTask(id, taskId),
    onSuccess: refresh,
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const p = pipeline.data
  const tasks = dag.data?.tasks ?? []
  const dependencies = dag.data?.dependencies ?? []

  return (
    <div className="space-y-6">
      <Link to="/pipelines" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> Pipelines
      </Link>

      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight">{p?.name ?? 'Pipeline'}</h1>
            {p && <PipelineStatusBadge status={p.status} />}
          </div>
          {p?.description && <p className="mt-1 text-muted-foreground">{p.description}</p>}
        </div>
        <div className="flex gap-2">
          {p?.status !== 'ACTIVE' ? (
            <Button variant="secondary" onClick={() => setStatus.mutate('ACTIVE')}>
              Activate
            </Button>
          ) : (
            <Button variant="secondary" onClick={() => setStatus.mutate('PAUSED')}>
              Pause
            </Button>
          )}
          <Button onClick={() => trigger.mutate()} disabled={trigger.isPending}>
            <Play className="mr-1 h-4 w-4" /> Trigger
          </Button>
          <Button variant="destructive" size="icon" onClick={() => remove.mutate()} aria-label="Delete">
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <Tabs defaultValue="dag">
        <TabsList>
          <TabsTrigger value="dag">DAG &amp; Tasks</TabsTrigger>
          <TabsTrigger value="config">Configuration</TabsTrigger>
        </TabsList>

        <TabsContent value="dag" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>DAG</CardTitle>
            </CardHeader>
            <CardContent className="h-[360px] p-0">
              <DagView tasks={tasks} dependencies={dependencies} />
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Add task</CardTitle>
              </CardHeader>
              <CardContent>
                <form
                  className="space-y-3"
                  onSubmit={(e: FormEvent) => {
                    e.preventDefault()
                    addTask.mutate()
                  }}
                >
                  <div className="space-y-2">
                    <Label>Task name</Label>
                    <Input required value={taskName} onChange={(e) => setTaskName(e.target.value)} />
                  </div>
                  <div className="space-y-2">
                    <Label>Type</Label>
                    <Select value={taskType} onValueChange={(v) => setTaskType(v as TaskType)}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {TASK_TYPES.map((t) => (
                          <SelectItem key={t} value={t}>
                            {t}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <label className="flex items-center gap-2 text-sm text-muted-foreground">
                    <input
                      type="checkbox"
                      checked={simulateFail}
                      onChange={(e) => setSimulateFail(e.target.checked)}
                    />
                    Simulate failure (for testing retries)
                  </label>
                  <Button type="submit" disabled={addTask.isPending}>
                    Add task
                  </Button>
                </form>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Add dependency</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="space-y-2">
                  <Label>Parent (runs first)</Label>
                  <Select value={parent} onValueChange={setParent}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select task" />
                    </SelectTrigger>
                    <SelectContent>
                      {tasks.map((t) => (
                        <SelectItem key={t.id} value={t.id}>
                          {t.taskName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Child (depends on parent)</Label>
                  <Select value={child} onValueChange={setChild}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select task" />
                    </SelectTrigger>
                    <SelectContent>
                      {tasks.map((t) => (
                        <SelectItem key={t.id} value={t.id}>
                          {t.taskName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <Button
                  disabled={!parent || !child || addDependency.isPending}
                  onClick={() => addDependency.mutate()}
                >
                  Add dependency
                </Button>
              </CardContent>
            </Card>
          </div>

          {tasks.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Tasks ({tasks.length})</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {tasks.map((t) => (
                  <div
                    key={t.id}
                    className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm"
                  >
                    <span className="font-medium">{t.taskName}</span>
                    <span className="text-xs uppercase text-muted-foreground">{t.taskType}</span>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => deleteTask.mutate(t.id)}
                      aria-label="Delete task"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="config">
          <Card>
            <CardHeader>
              <CardTitle>Configuration</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 text-sm">
              <Field label="Owner" value={p?.ownerName} />
              <Field label="Version" value={p?.version?.toString()} />
              <Field label="Schedule (cron)" value={p?.scheduleCron ?? '—'} mono />
              <Field
                label="Retry policy"
                value={p?.retryPolicy ? `${p.retryPolicy.maxRetries} retries, ${p.retryPolicy.backoffSeconds}s base` : '—'}
              />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

function Field({ label, value, mono }: { label: string; value?: string; mono?: boolean }) {
  return (
    <div>
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className={mono ? 'font-mono' : ''}>{value ?? '—'}</div>
    </div>
  )
}
