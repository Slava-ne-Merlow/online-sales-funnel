import { Suspense, lazy, type ReactNode } from 'react'
import { Navigate, Outlet, createBrowserRouter, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/auth-context'
import { AppShell } from '../widgets/layout/AppShell'
import { LoadingState } from '../shared/ui/States'

const DashboardPage = lazy(() =>
  import('../pages/DashboardPage').then((module) => ({ default: module.DashboardPage })),
)
const LoginPage = lazy(() =>
  import('../pages/LoginPage').then((module) => ({ default: module.LoginPage })),
)
const NotFoundPage = lazy(() =>
  import('../pages/NotFoundPage').then((module) => ({ default: module.NotFoundPage })),
)
const ProjectCreatePage = lazy(() =>
  import('../pages/ProjectCreatePage').then((module) => ({ default: module.ProjectCreatePage })),
)
const ProjectDetailsPage = lazy(() =>
  import('../pages/ProjectDetailsPage').then((module) => ({ default: module.ProjectDetailsPage })),
)
const ProjectsPage = lazy(() =>
  import('../pages/ProjectsPage').then((module) => ({ default: module.ProjectsPage })),
)
const AnalyticsPage = lazy(() =>
  import('../pages/AnalyticsPage').then((module) => ({ default: module.AnalyticsPage })),
)
const AdminActivityPage = lazy(() =>
  import('../pages/AdminActivityPage').then((module) => ({ default: module.AdminActivityPage })),
)

function RouteFallback() {
  return <LoadingState title="Загрузка" description="Подготавливаем экран." />
}

function withSuspense(node: ReactNode) {
  return <Suspense fallback={<RouteFallback />}>{node}</Suspense>
}

function ProtectedLayout() {
  const { isAuthenticated, isAuthReady } = useAuth()
  const location = useLocation()

  if (!isAuthReady) {
    return <LoadingState title="Загрузка профиля" description="Проверяем текущую сессию." />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }

  return (
    <AppShell>
      <Outlet />
    </AppShell>
  )
}

function AdminRoute() {
  const { user } = useAuth()

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}

function LoginRoute() {
  const { isAuthenticated, isAuthReady } = useAuth()

  if (!isAuthReady) {
    return <LoadingState title="Загрузка" description="Подготавливаем приложение." />
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return withSuspense(<LoginPage />)
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginRoute />,
  },
  {
    path: '/',
    element: <ProtectedLayout />,
    children: [
      {
        index: true,
        element: withSuspense(<DashboardPage />),
      },
      {
        path: 'projects',
        element: withSuspense(<ProjectsPage />),
      },
      {
        path: 'projects/new',
        element: withSuspense(<ProjectCreatePage />),
      },
      {
        path: 'projects/:projectId',
        element: withSuspense(<ProjectDetailsPage />),
      },
      {
        element: <AdminRoute />,
        children: [
          {
            path: 'analytics',
            element: withSuspense(<AnalyticsPage />),
          },
          {
            path: 'activity',
            element: withSuspense(<AdminActivityPage />),
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: withSuspense(<NotFoundPage />),
  },
])
