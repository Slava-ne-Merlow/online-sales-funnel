type StateProps = {
  title: string
  description?: string
}

export function LoadingState({ title, description }: StateProps) {
  return (
    <section className="state-card">
      <div className="spinner" />
      <h2>{title}</h2>
      {description ? <p>{description}</p> : null}
    </section>
  )
}

export function EmptyState({ title, description }: StateProps) {
  return (
    <section className="state-card">
      <h2>{title}</h2>
      {description ? <p>{description}</p> : null}
    </section>
  )
}

export function ErrorState({ title, description }: StateProps) {
  return (
    <section className="state-card state-card--error">
      <h2>{title}</h2>
      {description ? <p>{description}</p> : null}
    </section>
  )
}
