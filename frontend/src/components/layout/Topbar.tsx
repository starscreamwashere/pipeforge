import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Bell, LogOut, Server } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { workersApi } from '@/lib/services'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'

export function Topbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const { data: workers } = useQuery({
    queryKey: ['workers'],
    queryFn: workersApi.list,
    refetchInterval: 10_000,
  })
  const activeWorkers = workers?.filter((w) => w.status !== 'OFFLINE').length ?? 0

  const initials = (user?.name ?? '?')
    .split(' ')
    .map((p) => p[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <header className="sticky top-0 z-10 flex h-[72px] shrink-0 items-center justify-between border-b border-border bg-background/80 px-8 backdrop-blur">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Server className="h-4 w-4" />
        <span>
          <span className="font-medium text-foreground">{activeWorkers}</span> worker
          {activeWorkers === 1 ? '' : 's'} online
        </span>
      </div>

      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="h-[18px] w-[18px]" />
        </Button>
        <span className="rounded-full border border-border px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
          {user?.role}
        </span>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="flex items-center gap-2 rounded-full outline-none">
              <Avatar className="h-8 w-8">
                <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                  {initials}
                </AvatarFallback>
              </Avatar>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>
              <div className="font-medium">{user?.name}</div>
              <div className="text-xs font-normal text-muted-foreground">{user?.email}</div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => navigate('/settings')}>Settings</DropdownMenuItem>
            <DropdownMenuItem onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Log out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  )
}
