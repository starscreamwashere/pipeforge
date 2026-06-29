// Types mirroring the PipeForge backend DTOs (frozen API contracts).

export type Role = 'ADMIN' | 'ENGINEER' | 'VIEWER'

export interface User {
  id: string
  name: string
  email: string
  role: Role
  active: boolean
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type PipelineStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED'

export interface RetryPolicy {
  maxRetries: number
  backoffSeconds: number
}

export interface Pipeline {
  id: string
  name: string
  description: string | null
  scheduleCron: string | null
  retryPolicy: RetryPolicy | null
  ownerId: string
  ownerName: string
  status: PipelineStatus
  version: number
  createdAt: string
  updatedAt: string
}

export type TaskType = 'EXTRACT' | 'TRANSFORM' | 'LOAD' | 'CUSTOM'

export interface Task {
  id: string
  pipelineId: string
  taskName: string
  taskType: TaskType
  configPayload: Record<string, unknown> | null
  timeoutSeconds: number | null
  executionOrderHint: number | null
  createdAt: string
  updatedAt: string
}

export interface Dependency {
  id: string
  pipelineId: string
  parentTaskId: string
  childTaskId: string
}

export interface Dag {
  pipelineId: string
  tasks: Task[]
  dependencies: Dependency[]
  topologicalOrder: string[]
}

export type ExecutionStatus =
  | 'PENDING'
  | 'QUEUED'
  | 'RUNNING'
  | 'SUCCESS'
  | 'FAILED'
  | 'CANCELLED'
  | 'RETRYING'
  | 'FAILED_PERMANENTLY'

export type TriggerType = 'MANUAL' | 'CRON' | 'API'

export interface Execution {
  id: string
  pipelineId: string
  status: ExecutionStatus
  triggerType: TriggerType
  startedAt: string | null
  completedAt: string | null
  errorMessage: string | null
  createdAt: string
}

export interface TaskRun {
  id: string
  pipelineRunId: string
  taskId: string
  taskName: string
  status: ExecutionStatus
  attempt: number
  workerId: string | null
  startedAt: string | null
  completedAt: string | null
  errorMessage: string | null
}

export interface ExecutionDetail {
  execution: Execution
  taskRuns: TaskRun[]
}

export type WorkerStatus = 'ACTIVE' | 'BUSY' | 'OFFLINE'

export interface Worker {
  id: string
  name: string
  status: WorkerStatus
  jobsProcessed: number
  currentTaskRunId: string | null
  lastHeartbeatAt: string | null
  createdAt: string
}
