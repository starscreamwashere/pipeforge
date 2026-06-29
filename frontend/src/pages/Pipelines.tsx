import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { toast } from 'sonner'
import { pipelinesApi } from '@/lib/services'
import { apiErrorMessage } from '@/lib/api'
import { formatRelative } from '@/lib/format'
import { PipelineStatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'

export default function Pipelines() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [scheduleCron, setScheduleCron] = useState('')

  const { data, isLoading } = useQuery({ queryKey: ['pipelines', 0], queryFn: () => pipelinesApi.list(0, 50) })

  const createMutation = useMutation({
    mutationFn: () =>
      pipelinesApi.create({
        name,
        description: description || null,
        scheduleCron: scheduleCron || null,
      }),
    onSuccess: (pipeline) => {
      qc.invalidateQueries({ queryKey: ['pipelines'] })
      toast.success('Pipeline created')
      setOpen(false)
      setName('')
      setDescription('')
      setScheduleCron('')
      navigate(`/pipelines/${pipeline.id}`)
    },
    onError: (err) => toast.error(apiErrorMessage(err)),
  })

  function handleCreate(e: FormEvent) {
    e.preventDefault()
    createMutation.mutate()
  }

  const pipelines = data?.content ?? []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Pipelines</h1>
          <p className="text-muted-foreground">Create, schedule, and manage your data pipelines.</p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-1 h-4 w-4" /> New pipeline
            </Button>
          </DialogTrigger>
          <DialogContent>
            <form onSubmit={handleCreate}>
              <DialogHeader>
                <DialogTitle>Create pipeline</DialogTitle>
                <DialogDescription>Define metadata; add tasks and dependencies next.</DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label htmlFor="name">Name</Label>
                  <Input id="name" required value={name} onChange={(e) => setName(e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="description">Description</Label>
                  <Textarea id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="cron">Schedule (Quartz cron, optional)</Label>
                  <Input
                    id="cron"
                    placeholder="0 0 2 * * ?"
                    value={scheduleCron}
                    onChange={(e) => setScheduleCron(e.target.value)}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Creating…' : 'Create'}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Owner</TableHead>
              <TableHead>Schedule</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Updated</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={5}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : pipelines.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="py-12 text-center text-muted-foreground">
                  Create your first pipeline to get started.
                </TableCell>
              </TableRow>
            ) : (
              pipelines.map((p) => (
                <TableRow
                  key={p.id}
                  className="cursor-pointer"
                  onClick={() => navigate(`/pipelines/${p.id}`)}
                >
                  <TableCell className="font-medium">{p.name}</TableCell>
                  <TableCell className="text-muted-foreground">{p.ownerName}</TableCell>
                  <TableCell className="font-mono text-xs text-muted-foreground">
                    {p.scheduleCron ?? '—'}
                  </TableCell>
                  <TableCell>
                    <PipelineStatusBadge status={p.status} />
                  </TableCell>
                  <TableCell className="text-muted-foreground">{formatRelative(p.updatedAt)}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
