import { useMemo } from 'react'
import {
  Background,
  Controls,
  ReactFlow,
  type Edge,
  type Node,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { executionStatusColor } from '@/lib/format'
import type { Dependency, ExecutionStatus, Task } from '@/lib/types'

interface DagViewProps {
  tasks: Task[]
  dependencies: Dependency[]
  statusByTaskId?: Record<string, ExecutionStatus>
}

/** Lays nodes out in columns by dependency depth (longest path from a root). */
function computeLevels(tasks: Task[], dependencies: Dependency[]): Map<string, number> {
  const children = new Map<string, string[]>()
  const indegree = new Map<string, number>()
  tasks.forEach((t) => {
    children.set(t.id, [])
    indegree.set(t.id, 0)
  })
  dependencies.forEach((d) => {
    children.get(d.parentTaskId)?.push(d.childTaskId)
    indegree.set(d.childTaskId, (indegree.get(d.childTaskId) ?? 0) + 1)
  })

  const level = new Map<string, number>()
  const queue = tasks.filter((t) => (indegree.get(t.id) ?? 0) === 0).map((t) => t.id)
  queue.forEach((id) => level.set(id, 0))

  while (queue.length) {
    const id = queue.shift() as string
    const lvl = level.get(id) ?? 0
    for (const child of children.get(id) ?? []) {
      level.set(child, Math.max(level.get(child) ?? 0, lvl + 1))
      const remaining = (indegree.get(child) ?? 1) - 1
      indegree.set(child, remaining)
      if (remaining === 0) queue.push(child)
    }
  }
  tasks.forEach((t) => {
    if (!level.has(t.id)) level.set(t.id, 0)
  })
  return level
}

export function DagView({ tasks, dependencies, statusByTaskId }: DagViewProps) {
  const { nodes, edges } = useMemo(() => {
    const level = computeLevels(tasks, dependencies)
    const rowByLevel = new Map<number, number>()

    const nodes: Node[] = tasks.map((task) => {
      const lvl = level.get(task.id) ?? 0
      const row = rowByLevel.get(lvl) ?? 0
      rowByLevel.set(lvl, row + 1)
      const status = statusByTaskId?.[task.id]
      const color = status ? executionStatusColor(status) : 'var(--border)'
      return {
        id: task.id,
        position: { x: lvl * 240, y: row * 110 },
        data: {
          label: (
            <div className="text-left">
              <div className="text-sm font-medium text-foreground">{task.taskName}</div>
              <div className="text-[10px] uppercase tracking-wide text-muted-foreground">{task.taskType}</div>
              {status && (
                <div className="mt-1 text-[10px] font-medium" style={{ color }}>
                  {status.replace('_', ' ')}
                </div>
              )}
            </div>
          ),
        },
        style: {
          background: 'var(--card)',
          border: `1.5px solid ${color}`,
          borderRadius: 8,
          padding: '8px 12px',
          width: 180,
          color: 'var(--foreground)',
        },
      }
    })

    const edges: Edge[] = dependencies.map((dep) => ({
      id: dep.id,
      source: dep.parentTaskId,
      target: dep.childTaskId,
      animated: statusByTaskId?.[dep.childTaskId] === 'RUNNING',
      style: { stroke: 'var(--muted-foreground)' },
    }))

    return { nodes, edges }
  }, [tasks, dependencies, statusByTaskId])

  if (tasks.length === 0) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
        No tasks yet — add tasks to build the DAG.
      </div>
    )
  }

  return (
    <ReactFlow nodes={nodes} edges={edges} fitView proOptions={{ hideAttribution: true }} minZoom={0.2}>
      <Background color="var(--border)" gap={20} />
      <Controls showInteractive={false} />
    </ReactFlow>
  )
}
