import type { TimelineItem } from '../lib/project-history'

type TimelineProps = {
  items: TimelineItem[]
}

export function Timeline({ items }: TimelineProps) {
  return (
    <div className="timeline">
      {items.map((item) => (
        <article key={item.id} className="timeline__item">
          <div className="timeline__dot" />
          <div className="timeline__content">
            <div className="timeline__header">
              <h3>{item.title}</h3>
              <span>{item.createdAt}</span>
            </div>
            <div className="timeline__meta">{item.actor}</div>
            {item.changes.length ? (
              <ul className="timeline__changes">
                {item.changes.map((change) => (
                  <li key={change}>{change}</li>
                ))}
              </ul>
            ) : null}
            {item.comment ? <p className="timeline__comment">{item.comment}</p> : null}
          </div>
        </article>
      ))}
    </div>
  )
}
