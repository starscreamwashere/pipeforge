import { NavLink } from 'react-router-dom'
import {
  Activity,
  BarChart3,
  LayoutDashboard,
  ScrollText,
  Server,
  Settings,
  Workflow,
} from 'lucide-react'
import { cn } from '@/lib/utils'

const NAV = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/pipelines', label: 'Pipelines', icon: Workflow },
  { to: '/executions', label: 'Executions', icon: Activity },
  { to: '/workers', label: 'Workers', icon: Server },
  { to: '/logs', label: 'Logs', icon: ScrollText },
  { to: '/metrics', label: 'Metrics', icon: BarChart3 },
  { to: '/settings', label: 'Settings', icon: Settings },
]

export function Sidebar() {
  return (
    <aside className="flex h-svh w-[260px] shrink-0 flex-col border-r border-sidebar-border bg-sidebar">
      <div className="flex h-[72px] items-center gap-2.5 border-b border-sidebar-border px-6">
        <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary font-bold text-primary-foreground">
          P
        </div>
        <span className="text-lg font-semibold tracking-tight text-sidebar-foreground">PipeForge</span>
      </div>
      <nav className="flex flex-1 flex-col gap-1 p-3">
        {NAV.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-md border-l-2 border-transparent px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-foreground',
                isActive && 'border-primary bg-sidebar-accent text-sidebar-foreground',
              )
            }
          >
            <Icon className="h-[18px] w-[18px]" />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-sidebar-border p-4 text-xs text-muted-foreground">
        Data pipeline orchestration
      </div>
    </aside>
  )
}
