import type {
  ProjectEventType,
  ProjectSource,
  ProjectStage,
  ProjectStatus,
} from '../api/types'

export const projectStageOptions: Array<{ value: ProjectStage; label: string }> = [
  { value: 'QUALIFICATION', label: 'Проработка' },
  { value: 'PROPOSAL', label: 'КП' },
  { value: 'CONTRACTED', label: 'Законтрактован' },
  { value: 'INVOICE_ISSUED', label: 'Счет' },
  { value: 'WAITING_FOR_PAYMENT', label: 'Ожидаем доплату' },
]

export const projectStatusOptions: Array<{ value: ProjectStatus; label: string }> = [
  { value: 'ACTIVE', label: 'В работе' },
  { value: 'ON_HOLD', label: 'Отложена / заморожена' },
  { value: 'LOST', label: 'Проиграно / отказ' },
  { value: 'DONE', label: 'Успешна / завершена' },
  { value: 'INACTIVE', label: 'Не активна' },
]

export const projectSourceOptions: Array<{ value: ProjectSource; label: string }> = [
  { value: 'TENDER', label: 'Тендер' },
  { value: 'DIRECT_SALES', label: 'Прямые продажи' },
  { value: 'WEBSITE', label: 'Сайт' },
  { value: 'GK_OSTEK', label: 'ГК Остек' },
]

export const projectEventTypeLabels: Record<ProjectEventType, string> = {
  CREATED: 'Проект создан',
  STAGE_CHANGED: 'Изменён этап',
  STATUS_CHANGED: 'Изменён статус',
  STAGE_STATUS_CHANGED: 'Изменены этап и статус',
  AMOUNT_CHANGED: 'Изменена сумма',
  GLOBAL_COMMENT_CHANGED: 'Изменён общий комментарий',
  SOURCE_SET: 'Установлен источник',
  RESTORED_FROM_PAUSE: 'Возврат из паузы',
}

export const projectEventTypeOptions: Array<{ value: ProjectEventType; label: string }> = [
  { value: 'CREATED', label: 'Создание проекта' },
  { value: 'STAGE_CHANGED', label: 'Смена этапа' },
  { value: 'STATUS_CHANGED', label: 'Смена статуса' },
  { value: 'STAGE_STATUS_CHANGED', label: 'Смена этапа и статуса' },
  { value: 'AMOUNT_CHANGED', label: 'Изменение суммы' },
  { value: 'GLOBAL_COMMENT_CHANGED', label: 'Изменение комментария' },
  { value: 'SOURCE_SET', label: 'Изменение источника' },
  { value: 'RESTORED_FROM_PAUSE', label: 'Возобновление после паузы' },
]

export const stageOrder = projectStageOptions.map((option) => option.value)

function labelFrom<T extends string>(list: Array<{ value: T; label: string }>, value?: T | null) {
  if (!value) {
    return '—'
  }

  return list.find((item) => item.value === value)?.label ?? value
}

export function getProjectStageLabel(value?: ProjectStage | null) {
  return labelFrom(projectStageOptions, value)
}

export function getProjectStatusLabel(value?: ProjectStatus | null) {
  return labelFrom(projectStatusOptions, value)
}

export function getProjectSourceLabel(value?: ProjectSource | null) {
  return labelFrom(projectSourceOptions, value)
}

export function getEventTypeLabel(value: ProjectEventType) {
  return projectEventTypeLabels[value]
}

export function isBackwardStageChange(currentStage: ProjectStage, nextStage?: ProjectStage) {
  if (!nextStage) {
    return false
  }

  return stageOrder.indexOf(nextStage) < stageOrder.indexOf(currentStage)
}
