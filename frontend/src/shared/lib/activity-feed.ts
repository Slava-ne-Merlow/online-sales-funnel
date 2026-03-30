import type { Project, ProjectEventType, ProjectHistory } from '../api/types'
import { formatCurrency, formatDateTime } from './format'
import {
  getEventTypeLabel,
  getProjectSourceLabel,
  getProjectStageLabel,
  getProjectStatusLabel,
} from './project-meta'

export type ActivityFeedItem = {
  id: string
  rawCreatedAt: string
  createdAt: string
  actor: string
  eventType: ProjectEventType
  eventLabel: string
  projectId: string
  projectTitle: string
  summary: string
  comment?: string
}

function buildSummary(event: ProjectHistory, projectTitle: string) {
  switch (event.eventType) {
    case 'CREATED':
      return `Создал проект «${projectTitle}»`
    case 'STAGE_CHANGED':
      return `Перевёл проект «${projectTitle}» на этап «${getProjectStageLabel(event.newStage)}»`
    case 'STATUS_CHANGED':
      return `Изменил статус проекта «${projectTitle}» на «${getProjectStatusLabel(event.newStatus)}»`
    case 'STAGE_STATUS_CHANGED':
      return `Изменил этап и статус проекта «${projectTitle}»`
    case 'AMOUNT_CHANGED':
      return `Обновил сумму проекта «${projectTitle}»: ${formatCurrency(event.oldAmount)} → ${formatCurrency(event.newAmount)}`
    case 'GLOBAL_COMMENT_CHANGED':
      return `Обновил комментарий проекта «${projectTitle}»`
    case 'SOURCE_SET':
      return `Изменил источник проекта «${projectTitle}» на «${getProjectSourceLabel(event.newSource)}»`
    case 'RESTORED_FROM_PAUSE':
      return `Возобновил проект «${projectTitle}» после паузы`
    default:
      return `Обновил проект «${projectTitle}»`
  }
}

export function buildActivityFeedItems(projects: Project[], history: ProjectHistory[]) {
  const projectsMap = new Map(projects.map((project) => [project.id, project]))

  return [...history]
    .map<ActivityFeedItem | null>((event) => {
      const project = projectsMap.get(event.projectId)
      if (!project) {
        return null
      }

      const actor = event.actorUserName?.trim() || 'Сотрудник'
      return {
        id: event.id,
        rawCreatedAt: event.createdAt,
        createdAt: formatDateTime(event.createdAt),
        actor,
        eventType: event.eventType,
        eventLabel: getEventTypeLabel(event.eventType),
        projectId: event.projectId,
        projectTitle: project.title,
        summary: buildSummary(event, project.title),
        comment: event.comment,
      }
    })
    .filter(Boolean)
    .sort((left, right) => +new Date((right as ActivityFeedItem).rawCreatedAt) - +new Date((left as ActivityFeedItem).rawCreatedAt)) as ActivityFeedItem[]
}
