import { api } from './api'
import type {
  Dag,
  Dependency,
  Execution,
  ExecutionDetail,
  PageResponse,
  Pipeline,
  RetryPolicy,
  Task,
  TaskType,
  Worker,
} from './types'

// ─── Pipelines ────────────────────────────────────────────
export interface CreatePipelineInput {
  name: string
  description?: string | null
  scheduleCron?: string | null
  retryPolicy?: RetryPolicy | null
}

export const pipelinesApi = {
  list: (page = 0, size = 20) =>
    api.get<PageResponse<Pipeline>>('/api/v1/pipelines', { params: { page, size } }).then((r) => r.data),
  get: (id: string) => api.get<Pipeline>(`/api/v1/pipelines/${id}`).then((r) => r.data),
  create: (input: CreatePipelineInput) =>
    api.post<Pipeline>('/api/v1/pipelines', input).then((r) => r.data),
  update: (id: string, input: Partial<CreatePipelineInput> & { status?: string }) =>
    api.put<Pipeline>(`/api/v1/pipelines/${id}`, input).then((r) => r.data),
  remove: (id: string) => api.delete(`/api/v1/pipelines/${id}`).then((r) => r.data),
}

// ─── DAG ──────────────────────────────────────────────────
export interface CreateTaskInput {
  taskName: string
  taskType: TaskType
  configPayload?: Record<string, unknown> | null
  timeoutSeconds?: number | null
  executionOrderHint?: number | null
}

export const dagApi = {
  getDag: (pipelineId: string) =>
    api.get<Dag>(`/api/v1/pipelines/${pipelineId}/dag`).then((r) => r.data),
  listTasks: (pipelineId: string) =>
    api.get<Task[]>(`/api/v1/pipelines/${pipelineId}/tasks`).then((r) => r.data),
  addTask: (pipelineId: string, input: CreateTaskInput) =>
    api.post<Task>(`/api/v1/pipelines/${pipelineId}/tasks`, input).then((r) => r.data),
  deleteTask: (pipelineId: string, taskId: string) =>
    api.delete(`/api/v1/pipelines/${pipelineId}/tasks/${taskId}`).then((r) => r.data),
  addDependency: (pipelineId: string, parentTaskId: string, childTaskId: string) =>
    api
      .post<Dependency>(`/api/v1/pipelines/${pipelineId}/dependencies`, { parentTaskId, childTaskId })
      .then((r) => r.data),
  deleteDependency: (pipelineId: string, dependencyId: string) =>
    api.delete(`/api/v1/pipelines/${pipelineId}/dependencies/${dependencyId}`).then((r) => r.data),
}

// ─── Executions ───────────────────────────────────────────
export const executionsApi = {
  list: (page = 0, size = 20) =>
    api.get<PageResponse<Execution>>('/api/v1/executions', { params: { page, size } }).then((r) => r.data),
  get: (runId: string) => api.get<ExecutionDetail>(`/api/v1/executions/${runId}`).then((r) => r.data),
  trigger: (pipelineId: string) =>
    api.post<Execution>(`/api/v1/pipelines/${pipelineId}/trigger`, {}).then((r) => r.data),
  cancel: (runId: string) =>
    api.post<Execution>(`/api/v1/executions/${runId}/cancel`, {}).then((r) => r.data),
}

// ─── Workers ──────────────────────────────────────────────
export const workersApi = {
  list: () => api.get<Worker[]>('/api/v1/workers').then((r) => r.data),
}
