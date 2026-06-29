import type { ReactNode } from 'react'
import { Card, CardContent } from '@/components/ui/card'

export function KpiCard({
  label,
  value,
  icon,
  accent,
}: {
  label: string
  value: ReactNode
  icon?: ReactNode
  accent?: string
}) {
  return (
    <Card className="transition-shadow hover:shadow-lg">
      <CardContent className="flex items-center justify-between p-5">
        <div>
          <div className="text-sm text-muted-foreground">{label}</div>
          <div className="mt-1 text-3xl font-bold" style={accent ? { color: accent } : undefined}>
            {value}
          </div>
        </div>
        {icon && <div className="text-muted-foreground">{icon}</div>}
      </CardContent>
    </Card>
  )
}
