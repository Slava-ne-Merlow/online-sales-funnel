import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="auth-page">
      <section className="auth-card">
        <div className="auth-card__header">
          <h1>Страница не найдена</h1>
          <p>Проверьте адрес или вернитесь в CRM.</p>
        </div>
        <Link className="button button--primary" to="/">
          На главную
        </Link>
      </section>
    </div>
  )
}
