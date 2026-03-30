import type { ProjectHistory } from '../api/types'
import { formatCurrency, formatDateTime } from './format'
import {
  getEventTypeLabel,
  getProjectSourceLabel,
  getProjectStageLabel,
  getProjectStatusLabel,
} from './project-meta'

type UserOption = {
  value: string
  label: string
}

export type TimelineItem = {
  id: string
  title: string
  actor: string
  createdAt: string
  comment?: string
  changes: string[]
}

function resolveUserLabel(actorUserId: string, users: UserOption[]) {
  return users.find((user) => user.value === actorUserId)?.label ?? 'Пользователь'
}

function collectChanges(item: ProjectHistory) {
  const changes: string[] = []

  if (item.oldStage !== item.newStage && (item.oldStage || item.newStage)) {
    changes.push(`${getProjectStageLabel(item.oldStage)} → ${getProjectStageLabel(item.newStage)}`)
  }

  if (item.oldStatus !== item.newStatus && (item.oldStatus || item.newStatus)) {
    changes.push(`${getProjectStatusLabel(item.oldStatus)} → ${getProjectStatusLabel(item.newStatus)}`)
  }

  if (item.oldAmount !== item.newAmount && (item.oldAmount !== undefined || item.newAmount !== undefined)) {
    changes.push(`${formatCurrency(item.oldAmount)} → ${formatCurrency(item.newAmount)}`)
  }

  if (item.oldGlobalComment !== item.newGlobalComment && (item.oldGlobalComment || item.newGlobalComment)) {
    changes.push('Общий комментарий обновлён')
  }

  if (item.oldSource !== item.newSource && (item.oldSource || item.newSource)) {
    changes.push(`${getProjectSourceLabel(item.oldSource)} → ${getProjectSourceLabel(item.newSource)}`)
  }

  return changes
}

export function buildTimelineItems(history: ProjectHistory[], users: UserOption[]): TimelineItem[] {
  return [...history]
    .sort((left, right) => +new Date(right.createdAt) - +new Date(left.createdAt))
    .map((item) => ({
      id: item.id,
      title: getEventTypeLabel(item.eventType),
      actor: item.actorUserName?.trim() || resolveUserLabel(item.actorUserId, users),
      createdAt: formatDateTime(item.createdAt),
      comment: item.comment,
      changes: collectChanges(item),
    }))
}
