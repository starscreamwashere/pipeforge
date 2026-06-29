import { useAuth } from '@/auth/AuthContext'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function Settings() {
  const { user } = useAuth()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Settings</h1>
        <p className="text-muted-foreground">Your profile and account information.</p>
      </div>

      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Profile</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4 text-sm">
          <Field label="Name" value={user?.name} />
          <Field label="Email" value={user?.email} />
          <Field label="Role" value={user?.role} />
          <Field label="Status" value={user?.active ? 'Active' : 'Disabled'} />
        </CardContent>
      </Card>

      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>About</CardTitle>
        </CardHeader>
        <CardContent className="space-y-1 text-sm text-muted-foreground">
          <div>PipeForge — data pipeline orchestration platform</div>
          <div>Spring Boot 3 · Java 21 · PostgreSQL · Redis · React</div>
        </CardContent>
      </Card>
    </div>
  )
}

function Field({ label, value }: { label: string; value?: string }) {
  return (
    <div>
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="font-medium">{value ?? '—'}</div>
    </div>
  )
}
