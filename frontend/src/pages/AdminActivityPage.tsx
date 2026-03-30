import { useMemo, useState } from 'react'
import { useProjectsHistoryQuery, useProjectsQuery } from '../entities/project/queries'
import type { ProjectEventType } from '../shared/api/types'
import { buildActivityFeedItems } from '../shared/lib/activity-feed'
import { DateField } from '../shared/ui/DateField'
import { FilterBar } from '../shared/ui/FilterBar'
import { LoadingState, EmptyState, ErrorState } from '../shared/ui/States'
import { SelectField } from '../shared/ui/SelectField'
import { projectEventTypeOptions } from '../shared/lib/project-meta'
import { Button } from '../shared/ui/Button'
import { DataTable, type DataTableColumn } from '../shared/ui/DataTable'

const INITIAL_PROJECT_LIMIT = 20

type FiltersState = {
  dateFrom: string
  dateTo: string
  eventType: ProjectEventType | ''
  projectQuery: string
  actorQuery: string
}

const initialFilters: FiltersState = {
  dateFrom: '',
  dateTo: '',
  eventType: '',
  projectQuery: '',
  actorQuery: '',
}

export function AdminActivityPage() {
  const [filters, setFilters] = useState<FiltersState>(initialFilters)
  const [visibleProjectsCount, setVisibleProjectsCount] = useState(INITIAL_PROJECT_LIMIT)
  const projectsQuery = useProjectsQuery({
    sortBy: 'UPDATED_AT',
    sortDirection: 'DESC',
  })

  const visibleProjects = useMemo(() => {
    return (projectsQuery.data ?? []).slice(0, visibleProjectsCount)
  }, [projectsQuery.data, visibleProjectsCount])

  const historyQuery = useProjectsHistoryQuery(visibleProjects.map((project) => project.id))

  const activityItems = useMemo(() => {
    const items = buildActivityFeedItems(visibleProjects, historyQuery.data)

    return items.filter((item) => {
      if (filters.eventType && item.eventType !== filters.eventType) {
        return false
      }

      if (filters.projectQuery && !item.projectTitle.toLowerCase().includes(filters.projectQuery.toLowerCase())) {
        return false
      }

      if (filters.actorQuery && !item.actor.toLowerCase().includes(filters.actorQuery.toLowerCase())) {
        return false
      }

      if (filters.dateFrom) {
        const from = new Date(`${filters.dateFrom}T00:00:00`).getTime()
        if (new Date(item.rawCreatedAt).getTime() < from) {
          return false
        }
      }

      if (filters.dateTo) {
        const to = new Date(`${filters.dateTo}T23:59:59`).getTime()
        if (new Date(item.rawCreatedAt).getTime() > to) {
          return false
        }
      }

      return true
    })
  }, [filters, historyQuery.data, visibleProjects])

  const columns: Array<DataTableColumn<(typeof activityItems)[number]>> = [
    {
      key: 'createdAt',
      header: 'Дата и время',
      cell: (item) => item.createdAt,
    },
    {
      key: 'actor',
      header: 'Пользователь',
      cell: (item) => item.actor,
    },
    {
      key: 'event',
      header: 'Событие',
      cell: (item) => item.eventLabel,
    },
    {
      key: 'project',
      header: 'Проект',
      cell: (item) => item.projectTitle,
    },
    {
      key: 'summary',
      header: 'Описание',
      cell: (item) => item.summary,
    },
    {
      key: 'comment',
      header: 'Комментарий',
      cell: (item) => item.comment || '—',
    },
  ]

  if (projectsQuery.isPending || historyQuery.isPending) {
    return <LoadingState title="Загрузка истории действий" description="Собираем общую ленту по последним проектам." />
  }

  if (projectsQuery.isError || historyQuery.isError) {
    return <ErrorState title="Не удалось загрузить историю действий" description="Попробуйте обновить страницу." />
  }

  return (
    <div className="page-stack">
      <FilterBar
        title="История действий"
        actions={
          <>
            <Button variant="secondary" onClick={() => setFilters(initialFilters)}>
              Сбросить
            </Button>
            {visibleProjects.length < (projectsQuery.data?.length ?? 0) ? (
              <Button variant="ghost" onClick={() => setVisibleProjectsCount((current) => current + INITIAL_PROJECT_LIMIT)}>
                Загрузить ещё проекты
              </Button>
            ) : null}
          </>
        }
      >
        <label className="field">
          <span>Период с</span>
          <DateField value={filters.dateFrom} onChange={(value) => setFilters((current) => ({ ...current, dateFrom: value }))} />
        </label>

        <label className="field">
          <span>Период по</span>
          <DateField value={filters.dateTo} onChange={(value) => setFilters((current) => ({ ...current, dateTo: value }))} />
        </label>

        <label className="field">
          <span>Тип события</span>
          <SelectField
            value={filters.eventType}
            onChange={(value) => setFilters((current) => ({ ...current, eventType: value as ProjectEventType | '' }))}
            options={[{ value: '', label: 'Все события' }, ...projectEventTypeOptions]}
          />
        </label>

        <label className="field">
          <span>Проект</span>
          <input
            value={filters.projectQuery}
            onChange={(event) => setFilters((current) => ({ ...current, projectQuery: event.target.value }))}
            placeholder="Поиск по названию"
          />
        </label>

        <label className="field">
          <span>Пользователь</span>
          <input
            value={filters.actorQuery}
            onChange={(event) => setFilters((current) => ({ ...current, actorQuery: event.target.value }))}
            placeholder="Поиск по имени"
          />
        </label>
      </FilterBar>

      <section className="panel">
        <div className="panel__header">
          <div>
            <h2 className="section-title">Журнал действий</h2>
            <p className="section-hint">
              Лента собрана по {visibleProjects.length} последним обновлённым проектам из общего списка.
            </p>
          </div>
        </div>

        {activityItems.length ? (
          <DataTable columns={columns} rows={activityItems} getRowKey={(item) => item.id} />
        ) : (
          <EmptyState title="События не найдены" description="Попробуйте ослабить фильтр или загрузить больше проектов." />
        )}
      </section>
    </div>
  )
}
