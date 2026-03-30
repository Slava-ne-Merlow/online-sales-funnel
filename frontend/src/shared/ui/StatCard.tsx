import type { ReactNode } from 'react'

type StatCardProps = {
  title: string
  value: ReactNode
  hint?: string
}

export function StatCard({ title, value, hint }: StatCardProps) {
  return (
    <article className="stat-card">
      <div className="stat-card__label">{title}</div>
      <div className="stat-card__value">{value}</div>
      {hint ? <div className="stat-card__hint">{hint}</div> : null}
    </article>
  )
}
