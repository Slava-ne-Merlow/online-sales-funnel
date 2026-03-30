import { useMemo, useState } from 'react'
import { useProjectsHistoryQuery, useProjectsLookupQuery } from '../entities/project/queries'
import { useUsersQuery } from '../entities/user/queries'
import { useAuth } from '../features/auth/auth-context'
import { formatCurrency, formatDateTime } from '../shared/lib/format'
import {
  buildUserOptions,
  collectRecentActivity,
  filterProjectsForDashboard,
  resolveUserLabel,
  type DashboardFilters,
} from '../shared/lib/project-analytics'
import {
  getEventTypeLabel,
  getProjectStageLabel,
  getProjectStatusLabel,
  projectSourceOptions,
  projectStageOptions,
  projectStatusOptions,
} from '../shared/lib/project-meta'
import { ComboboxField } from '../shared/ui/ComboboxField'
import { DataTable, type DataTableColumn } from '../shared/ui/DataTable'
import { DateField } from '../shared/ui/DateField'
import { EmptyState, ErrorState, LoadingState } from '../shared/ui/States'
import { FilterBar } from '../shared/ui/FilterBar'
import { SelectField } from '../shared/ui/SelectField'
import { StatCard } from '../shared/ui/StatCard'

type DistributionRow = {
  key: string
  label: string
  count: number
  amount: number
}

export function DashboardPage() {
  const { user } = useAuth()
  const [filters, setFilters] = useState<DashboardFilters>({
    source: '',
    stage: '',
    status: '',
    userId: '',
  })
  const usersQuery = useUsersQuery(user?.role === 'ADMIN')
  const projectsQuery = useProjectsLookupQuery()
  const historyQuery = useProjectsHistoryQuery((projectsQuery.data ?? []).map((project) => project.id))

  const userOptions = useMemo(() => {
    return buildUserOptions({
      users: usersQuery.data ?? [],
      projects: projectsQuery.data ?? [],
      history: historyQuery.data ?? [],
      currentUser: user,
    })
  }, [historyQuery.data, projectsQuery.data, user, usersQuery.data])

  const filteredProjects = useMemo(() => {
    return filterProjectsForDashboard(projectsQuery.data ?? [], filters)
  }, [filters, projectsQuery.data])

  const filteredProjectIds = useMemo(() => new Set(filteredProjects.map((project) => project.id)), [filteredProjects])

  const filteredHistory = useMemo(() => {
    return collectRecentActivity(
      (historyQuery.data ?? []).filter((item) => filteredProjectIds.has(item.projectId)),
      filters,
    )
  }, [filteredProjectIds, filters, historyQuery.data])

  const statusRows = useMemo<DistributionRow[]>(() => {
    return projectStatusOptions.map((option) => ({
      key: option.value,
      label: option.label,
      count: filteredProjects.filter((project) => project.currentStatus === option.value).length,
      amount: filteredProjects
        .filter((project) => project.currentStatus === option.value)
        .reduce((sum, project) => sum + (project.currentAmount ?? 0), 0),
    }))
  }, [filteredProjects])

  const stageRows = useMemo<DistributionRow[]>(() => {
    return projectStageOptions.map((option) => ({
      key: option.value,
      label: option.label,
      count: filteredProjects.filter((project) => project.currentStage === option.value).length,
      amount: filteredProjects
        .filter((project) => project.currentStage === option.value)
        .reduce((sum, project) => sum + (project.currentAmount ?? 0), 0),
    }))
  }, [filteredProjects])

  const sourceRows = useMemo<DistributionRow[]>(() => {
    return projectSourceOptions.map((option) => ({
      key: option.value,
      label: option.label,
      count: filteredProjects.filter((project) => project.source === option.value).length,
      amount: filteredProjects
        .filter((project) => project.source === option.value)
        .reduce((sum, project) => sum + (project.currentAmount ?? 0), 0),
    }))
  }, [filteredProjects])

  const recentProjects = useMemo(() => {
    return [...filteredProjects].sort((left, right) => +new Date(right.updatedAt) - +new Date(left.updatedAt)).slice(0, 5)
  }, [filteredProjects])

  const recentActivity = filteredHistory.slice(0, 8)
  const pipelineAmount = filteredProjects
    .filter((project) => project.currentStatus === 'ACTIVE' || project.currentStatus === 'ON_HOLD')
    .reduce((sum, project) => sum + (project.currentAmount ?? 0), 0)

  const largestProjects = [...filteredProjects]
    .sort((left, right) => (right.currentAmount ?? 0) - (left.currentAmount ?? 0))
    .slice(0, 5)

  const projectColumns: Array<DataTableColumn<(typeof recentProjects)[number]>> = [
    {
      key: 'title',
      header: 'Проект',
      cell: (project) => project.title,
    },
    {
      key: 'status',
      header: 'Статус',
      cell: (project) => getProjectStatusLabel(project.currentStatus),
    },
    {
      key: 'stage',
      header: 'Этап',
      cell: (project) => getProjectStageLabel(project.currentStage),
    },
    {
      key: 'amount',
      header: 'Сумма',
      cell: (project) => formatCurrency(project.currentAmount),
      className: 'table-cell--numeric',
    },
    {
      key: 'updatedAt',
      header: 'Обновлён',
      cell: (project) => formatDateTime(project.updatedAt),
    },
    {
      key: 'responsible',
      header: 'Ответственный',
      cell: (project) => project.responsibleUserName || 'Не назначен',
    },
  ]

  const activityColumns: Array<DataTableColumn<(typeof recentActivity)[number]>> = [
    {
      key: 'event',
      header: 'Событие',
      cell: (item) => getEventTypeLabel(item.eventType),
    },
    {
      key: 'actor',
      header: 'Исполнитель',
      cell: (item) => resolveUserLabel(item.actorUserId, userOptions, item.actorUserName),
    },
    {
      key: 'createdAt',
      header: 'Время',
      cell: (item) => formatDateTime(item.createdAt),
    },
    {
      key: 'comment',
      header: 'Комментарий',
      cell: (item) => item.comment || '—',
    },
  ]

  const distributionColumns: Array<DataTableColumn<DistributionRow>> = [
    {
      key: 'label',
      header: 'Сегмент',
      cell: (item) => item.label,
    },
    {
      key: 'count',
      header: 'Кол-во',
      cell: (item) => item.count,
      className: 'table-cell--numeric',
    },
    {
      key: 'amount',
      header: 'Сумма',
      cell: (item) => formatCurrency(item.amount),
      className: 'table-cell--numeric',
    },
  ]

  if (projectsQuery.isPending || historyQuery.isPending) {
    return <LoadingState title="Загрузка dashboard" description="Собираем проекты и историю для аналитики." />
  }

  if (projectsQuery.isError || historyQuery.isError) {
    return <ErrorState title="Не удалось собрать dashboard" description="Проверьте backend и доступ к history endpoints." />
  }

  return (
    <div className="page-stack">
      {user?.role === 'ADMIN' ? (
        <FilterBar title="Фильтры dashboard">
          <label className="field">
            <span>Дата с</span>
            <DateField
              value={filters.createdAtFrom?.slice(0, 10) ?? ''}
              onChange={(value) =>
                setFilters((current) => ({
                  ...current,
                  createdAtFrom: value ? `${value}T00:00:00.000Z` : undefined,
                }))
              }
            />
          </label>
          <label className="field">
            <span>Дата по</span>
            <DateField
              value={filters.createdAtTo?.slice(0, 10) ?? ''}
              onChange={(value) =>
                setFilters((current) => ({
                  ...current,
                  createdAtTo: value ? `${value}T23:59:59.000Z` : undefined,
                }))
              }
            />
          </label>
          <label className="field">
            <span>Источник</span>
            <SelectField
              value={filters.source ?? ''}
              onChange={(value) =>
                setFilters((current) => ({ ...current, source: value as DashboardFilters['source'] }))
              }
              options={projectSourceOptions}
              placeholder="Все источники"
            />
          </label>
          <label className="field">
            <span>Этап</span>
            <SelectField
              value={filters.stage ?? ''}
              onChange={(value) =>
                setFilters((current) => ({ ...current, stage: value as DashboardFilters['stage'] }))
              }
              options={projectStageOptions}
              placeholder="Все этапы"
            />
          </label>
          <label className="field">
            <span>Статус</span>
            <SelectField
              value={filters.status ?? ''}
              onChange={(value) =>
                setFilters((current) => ({ ...current, status: value as DashboardFilters['status'] }))
              }
              options={projectStatusOptions}
              placeholder="Все статусы"
            />
          </label>
          <label className="field">
            <span>Ответственный</span>
            <ComboboxField
              value={filters.userId ?? ''}
              onChange={(value) => setFilters((current) => ({ ...current, userId: value || undefined }))}
              options={userOptions}
              placeholder="Все сотрудники"
            />
          </label>
        </FilterBar>
      ) : null}

      <section className="stats-grid">
        <StatCard title="Всего проектов" value={filteredProjects.length} />
        <StatCard
          title={user?.role === 'ADMIN' ? 'Pipeline amount' : 'Активный портфель'}
          value={formatCurrency(pipelineAmount)}
        />
        <StatCard title="В работе / отложены" value={`${statusRows.find((row) => row.key === 'ACTIVE')?.count ?? 0} / ${statusRows.find((row) => row.key === 'ON_HOLD')?.count ?? 0}`} />
        <StatCard title="Не активны / завершены" value={`${statusRows.find((row) => row.key === 'INACTIVE')?.count ?? 0} / ${statusRows.find((row) => row.key === 'DONE')?.count ?? 0}`} />
      </section>

      {filteredProjects.length ? (
        <>
          <div className="content-grid">
            <section className="panel">
              <div className="panel__header">
                <div>
                  <h2 className="section-title">{user?.role === 'ADMIN' ? 'Статистика по статусам' : 'Статусы'}</h2>
                </div>
              </div>
              <DataTable columns={distributionColumns} rows={statusRows} getRowKey={(row) => row.key} />
            </section>

            <section className="panel">
              <div className="panel__header">
                <div>
                  <h2 className="section-title">{user?.role === 'ADMIN' ? 'Статистика по этапам' : 'Разбивка по этапам'}</h2>
                </div>
              </div>
              <DataTable columns={distributionColumns} rows={stageRows} getRowKey={(row) => row.key} />
            </section>
          </div>

          {user?.role === 'ADMIN' ? (
            <section className="panel">
              <div className="panel__header">
                <div>
                  <h2 className="section-title">Статистика по источнику</h2>
                </div>
              </div>
              <DataTable columns={distributionColumns} rows={sourceRows} getRowKey={(row) => row.key} />
            </section>
          ) : null}

          <div className="content-grid">
            <section className="panel">
              <div className="panel__header">
                <div>
                  <h2 className="section-title">Recent projects</h2>
                </div>
              </div>
              <DataTable columns={projectColumns} rows={recentProjects} getRowKey={(project) => project.id} />
            </section>

            <section className="panel">
              <div className="panel__header">
                <div>
                  <h2 className="section-title">{user?.role === 'ADMIN' ? 'Recent transitions' : 'Recent activity'}</h2>
                </div>
              </div>
              <DataTable columns={activityColumns} rows={recentActivity} getRowKey={(item) => item.id} />
            </section>
          </div>

          {user?.role === 'ADMIN' ? (
            <section className="panel">
              <div className="panel__header">
                <div>
                  <h2 className="section-title">Largest projects</h2>
                </div>
              </div>
              <DataTable columns={projectColumns} rows={largestProjects} getRowKey={(project) => project.id} />
            </section>
          ) : null}
        </>
      ) : (
        <EmptyState title="Данных нет" description="Нет проектов, подходящих под текущий фильтр." />
      )}
    </div>
  )
}
