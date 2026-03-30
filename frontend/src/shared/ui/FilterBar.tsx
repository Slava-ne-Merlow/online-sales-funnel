import type { PropsWithChildren, ReactNode } from 'react'

type FilterBarProps = PropsWithChildren<{
  title?: string
  actions?: ReactNode
}>

export function FilterBar({ title, actions, children }: FilterBarProps) {
  return (
    <section className="panel">
      <div className="panel__header">
        <div>
          {title ? <h2 className="section-title">{title}</h2> : null}
        </div>
        {actions ? <div className="panel__actions">{actions}</div> : null}
      </div>
      <div className="filter-grid">{children}</div>
    </section>
  )
}
