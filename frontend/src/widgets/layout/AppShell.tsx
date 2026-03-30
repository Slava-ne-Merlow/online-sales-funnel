import type { PropsWithChildren } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../../features/auth/auth-context'
import { Button } from '../../shared/ui/Button'

const titles: Record<string, string> = {
  '/': 'Сводка',
  '/projects': 'Проекты',
  '/projects/new': 'Новый проект',
  '/analytics': 'Аналитика',
  '/activity': 'История действий',
}

export function AppShell({ children }: PropsWithChildren) {
  const location = useLocation()
  const { user, logout } = useAuth()

  const pageTitle = titles[location.pathname] ?? 'CRM'

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <div className="sidebar__title">Production CRM</div>
          <div className="sidebar__subtitle">Воронка продаж предприятия</div>
        </div>

        <nav className="sidebar__nav">
          <NavLink to="/" end className="sidebar__link">
            Сводка
          </NavLink>
          <NavLink to="/projects" className="sidebar__link">
            Проекты
          </NavLink>
          {user?.role === 'ADMIN' ? (
            <>
              <NavLink to="/analytics" className="sidebar__link">
                Аналитика
              </NavLink>
              <NavLink to="/activity" className="sidebar__link">
                История действий
              </NavLink>
            </>
          ) : null}
        </nav>
      </aside>

      <div className="shell__content">
        <header className="topbar">
          <div>
            <div className="topbar__eyebrow">CRM модуль</div>
            <h1 className="topbar__title">{pageTitle}</h1>
          </div>

          <div className="user-menu">
            <div>
              <div className="user-menu__name">{user?.name}</div>
              <div className="user-menu__meta">
                {user?.email} · {user?.role}
              </div>
            </div>
            <Button variant="secondary" onClick={logout}>
              Выйти
            </Button>
          </div>
        </header>

        <main className="page">{children}</main>
      </div>
    </div>
  )
}
