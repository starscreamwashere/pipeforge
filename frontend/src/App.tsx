const EXECUTION_STATES = [
  { label: 'PENDING', token: 'var(--state-pending)' },
  { label: 'QUEUED', token: 'var(--state-queued)' },
  { label: 'RUNNING', token: 'var(--state-running)' },
  { label: 'SUCCESS', token: 'var(--state-success)' },
  { label: 'FAILED', token: 'var(--state-failed)' },
  { label: 'RETRYING', token: 'var(--state-retrying)' },
  { label: 'CANCELLED', token: 'var(--state-cancelled)' },
] as const

function App() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-background px-8 text-foreground">
      <div className="text-center">
        <h1 className="text-[32px] font-bold tracking-tight">PipeForge</h1>
        <p className="mt-2 text-muted-foreground">
          Data pipeline orchestration — frontend bootstrap (Milestone 0.3)
        </p>
      </div>
      <div className="flex flex-wrap items-center justify-center gap-2">
        {EXECUTION_STATES.map((s) => (
          <span
            key={s.label}
            className="rounded-md px-3 py-1 font-mono text-xs font-medium text-background"
            style={{ backgroundColor: s.token }}
          >
            {s.label}
          </span>
        ))}
      </div>
    </div>
  )
}

export default App
