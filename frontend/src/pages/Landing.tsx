import { Link } from 'react-router-dom'
import { Activity, GitBranch, RefreshCw, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'

const FEATURES = [
  { icon: GitBranch, title: 'DAG pipelines', text: 'Define tasks and dependencies; cycles are rejected automatically.' },
  { icon: Activity, title: 'Distributed execution', text: 'Workers claim jobs from Redis and run them on virtual threads.' },
  { icon: RefreshCw, title: 'Retries & recovery', text: 'Exponential backoff with dead-letter handling for failed tasks.' },
  { icon: ShieldCheck, title: 'Observability', text: 'Metrics, structured logs, and live execution monitoring.' },
]

export default function Landing() {
  return (
    <div className="min-h-svh bg-background text-foreground">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-8 py-6">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary font-bold text-primary-foreground">
            P
          </div>
          <span className="text-lg font-semibold">PipeForge</span>
        </div>
        <div className="flex gap-3">
          <Button asChild variant="ghost">
            <Link to="/login">Log in</Link>
          </Button>
          <Button asChild>
            <Link to="/signup">Sign up</Link>
          </Button>
        </div>
      </header>

      <section className="mx-auto max-w-3xl px-8 py-24 text-center">
        <span className="rounded-full border border-border px-3 py-1 text-xs text-muted-foreground">
          Production-grade pipeline orchestration
        </span>
        <h1 className="mt-6 text-5xl font-bold tracking-tight">
          Schedule, run, and recover distributed data pipelines.
        </h1>
        <p className="mx-auto mt-5 max-w-xl text-lg text-muted-foreground">
          PipeForge orchestrates DAG-based ETL workflows with cron scheduling, distributed workers,
          automatic retries, and full observability.
        </p>
        <div className="mt-8 flex justify-center gap-4">
          <Button asChild size="lg">
            <Link to="/signup">Get started</Link>
          </Button>
          <Button asChild size="lg" variant="secondary">
            <Link to="/login">Log in</Link>
          </Button>
        </div>
      </section>

      <section className="mx-auto grid max-w-5xl grid-cols-1 gap-4 px-8 pb-24 sm:grid-cols-2 lg:grid-cols-4">
        {FEATURES.map(({ icon: Icon, title, text }) => (
          <div key={title} className="rounded-lg border border-border bg-card p-5">
            <Icon className="h-6 w-6 text-primary" />
            <h3 className="mt-3 font-semibold">{title}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{text}</p>
          </div>
        ))}
      </section>
    </div>
  )
}
