import type { Project, ProjectHistory, ProjectSource, ProjectStage, ProjectStatus, User } from '../api/types'

export type DashboardFilters = {
  createdAtFrom?: string
  createdAtTo?: string
  source?: ProjectSource | ''
  stage?: ProjectStage | ''
  status?: ProjectStatus | ''
  userId?: string
}

export type UserOption = {
  value: string
  label: string
  description?: string
}

type BuildUserOptionsParams = {
  projects?: Project[]
  history?: ProjectHistory[]
  users?: User[]
  currentUser?: User | null
}

export function buildUserOptions({
  projects = [],
  history = [],
  users = [],
  currentUser = null,
}: BuildUserOptionsParams) {
  const options = new Map<string, UserOption>()

  const upsert = (value?: string, label?: string | null, description?: string) => {
    if (!value) {
      return
    }

    const safeLabel = label?.trim() || 'Пользователь'
    const existing = options.get(value)

    options.set(value, {
      value,
      label: currentUser?.id === value ? `${safeLabel} (вы)` : safeLabel,
      description: description ?? existing?.description,
    })
  }

  users.forEach((user) => upsert(user.id, user.name, user.email))
  if (currentUser) {
    upsert(currentUser.id, currentUser.name, currentUser.email)
  }

  projects.forEach((project) => {
    upsert(project.createdById, project.createdByName)
    upsert(project.responsibleUserId, project.responsibleUserName)
  })

  history.forEach((event) => upsert(event.actorUserId, event.actorUserName))

  return Array.from(options.values()).sort((left, right) => left.label.localeCompare(right.label, 'ru'))
}

export function resolveUserLabel(
  userId: string | undefined,
  options: UserOption[],
  fallbackName?: string | null,
) {
  if (fallbackName?.trim()) {
    return fallbackName
  }

  if (!userId) {
    return 'Не назначен'
  }

  return options.find((option) => option.value === userId)?.label ?? 'Пользователь'
}

export function filterProjectsForDashboard(projects: Project[], filters: DashboardFilters) {
  return projects.filter((project) => {
    const createdAt = new Date(project.createdAt).getTime()

    if (filters.createdAtFrom && createdAt < new Date(filters.createdAtFrom).getTime()) {
      return false
    }

    if (filters.createdAtTo && createdAt > new Date(filters.createdAtTo).getTime()) {
      return false
    }

    if (filters.source && project.source !== filters.source) {
      return false
    }

    if (filters.stage && project.currentStage !== filters.stage) {
      return false
    }

    if (filters.status && project.currentStatus !== filters.status) {
      return false
    }

    if (filters.userId) {
      const matchesCreatedBy = project.createdById === filters.userId
      const matchesResponsible = project.responsibleUserId === filters.userId

      if (!matchesCreatedBy && !matchesResponsible) {
        return false
      }
    }

    return true
  })
}

export function collectRecentActivity(history: ProjectHistory[], filters?: DashboardFilters) {
  return [...history]
    .filter((item) => {
      if (!filters?.userId) {
        return true
      }

      return item.actorUserId === filters.userId
    })
    .sort((left, right) => +new Date(right.createdAt) - +new Date(left.createdAt))
}
