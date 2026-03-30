import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useProjectsLookupQuery, useProjectsQuery } from '../entities/project/queries'
import { useUsersQuery } from '../entities/user/queries'
import { useAuth } from '../features/auth/auth-context'
import type { GetProjectsParams, Project, ProjectSortDirection, ProjectSortField } from '../shared/api/types'
import { formatCurrency, formatDate, toDateTimeEnd, toDateTimeStart } from '../shared/lib/format'
import { buildUserOptions, resolveUserLabel } from '../shared/lib/project-analytics'
import {
  getProjectSourceLabel,
  getProjectStageLabel,
  getProjectStatusLabel,
  projectSourceOptions,
  projectStageOptions,
  projectStatusOptions,
} from '../shared/lib/project-meta'
import { Button } from '../shared/ui/Button'
import { ComboboxField } from '../shared/ui/ComboboxField'
import { DataTable, type DataTableColumn } from '../shared/ui/DataTable'
import { DateField } from '../shared/ui/DateField'
import { EmptyState, ErrorState, LoadingState } from '../shared/ui/States'
import { FilterBar } from '../shared/ui/FilterBar'
import { SelectField } from '../shared/ui/SelectField'
import { StatusChip } from '../shared/ui/StatusChip'

type FiltersState = {
  currentStage: Project['currentStage'] | ''
  currentStatus: Project['currentStatus'] | ''
  source: Project['source'] | ''
  responsibleUser: string
  createdAtFrom: string
  createdAtTo: string
  sortBy: ProjectSortField
  sortDirection: ProjectSortDirection
}

const initialFilters: FiltersState = {
  currentStage: '',
  currentStatus: '',
  source: '',
  responsibleUser: '',
  createdAtFrom: '',
  createdAtTo: '',
  sortBy: 'UPDATED_AT',
  sortDirection: 'DESC',
}

function getStatusTone(status: Project['currentStatus']) {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'ON_HOLD':
      return 'warning'
    case 'LOST':
      return 'danger'
    case 'INACTIVE':
      return 'neutral'
    case 'DONE':
      return 'info'
    default:
      return 'neutral'
  }
}

export function ProjectsPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [filters, setFilters] = useState<FiltersState>(initialFilters)
  const lookupQuery = useProjectsLookupQuery()
  const isAdmin = user?.role === 'ADMIN'
  const usersQuery = useUsersQuery(isAdmin)

  const queryFilters = useMemo<GetProjectsParams>(
    () => ({
      currentStage: filters.currentStage || undefined,
      currentStatus: filters.currentStatus || undefined,
      source: filters.source || undefined,
      responsibleUser: isAdmin ? filters.responsibleUser || undefined : undefined,
      createdAtFrom: toDateTimeStart(filters.createdAtFrom),
      createdAtTo: toDateTimeEnd(filters.createdAtTo),
      sortBy: filters.sortBy,
      sortDirection: filters.sortDirection,
    }),
    [filters, isAdmin],
  )

  const projectsQuery = useProjectsQuery(queryFilters)

  const userOptions = useMemo(
    () =>
      buildUserOptions({
        users: usersQuery.data ?? [],
        projects: lookupQuery.data ?? [],
        currentUser: user,
      }),
    [lookupQuery.data, user, usersQuery.data],
  )

  const columns: Array<DataTableColumn<Project>> = [
    {
      key: 'title',
      header: 'Проект',
      cell: (project) => <div className="table-primary">{project.title}</div>,
    },
    {
      key: 'source',
      header: 'Источник',
      cell: (project) => getProjectSourceLabel(project.source),
    },
    {
      key: 'comment',
      header: 'Комментарий',
      cell: (project) =>
        project.globalComment ? (
          <div className="table-cell--comment" title={project.globalComment}>
            {project.globalComment}
          </div>
        ) : (
          '—'
        ),
    },
    {
      key: 'amount',
      header: 'Текущая сумма',
      cell: (project) => formatCurrency(project.currentAmount),
      className: 'table-cell--numeric',
    },
    {
      key: 'stage',
      header: 'Этап',
      cell: (project) => <StatusChip label={getProjectStageLabel(project.currentStage)} tone="neutral" />,
    },
    {
      key: 'status',
      header: 'Статус',
      cell: (project) => (
        <StatusChip label={getProjectStatusLabel(project.currentStatus)} tone={getStatusTone(project.currentStatus)} />
      ),
    },
    {
      key: 'responsible',
      header: 'Ответственный',
      cell: (project) => resolveUserLabel(project.responsibleUserId, userOptions, project.responsibleUserName),
    },
    {
      key: 'createdAt',
      header: 'Создан',
      cell: (project) => formatDate(project.createdAt),
    },
    {
      key: 'updatedAt',
      header: 'Обновлён',
      cell: (project) => formatDate(project.updatedAt),
    },
  ]

  if (projectsQuery.isPending) {
    return <LoadingState title="Загрузка проектов" description="Подготавливаем список проектов." />
  }

  if (projectsQuery.isError) {
    return <ErrorState title="Не удалось загрузить проекты" description="Попробуйте обновить страницу чуть позже." />
  }

  return (
    <div className="page-stack">
      <FilterBar
        title="Проекты"
        actions={
          <>
            <Button variant="secondary" onClick={() => setFilters(initialFilters)}>
              Сбросить
            </Button>
            <Button onClick={() => navigate('/projects/new')}>Новый проект</Button>
          </>
        }
      >
        <label className="field">
          <span>Этап</span>
          <SelectField
            value={filters.currentStage}
            onChange={(value) => setFilters((current) => ({ ...current, currentStage: value }))}
            options={projectStageOptions}
            placeholder="Все этапы"
          />
        </label>

        <label className="field">
          <span>Статус</span>
          <SelectField
            value={filters.currentStatus}
            onChange={(value) => setFilters((current) => ({ ...current, currentStatus: value }))}
            options={projectStatusOptions}
            placeholder="Все статусы"
          />
        </label>

        <label className="field">
          <span>Источник</span>
          <SelectField
            value={filters.source}
            onChange={(value) => setFilters((current) => ({ ...current, source: value }))}
            options={[{ value: '', label: 'Все источники' }, ...projectSourceOptions]}
          />
        </label>

        {isAdmin ? (
          <label className="field">
            <span>Ответственный</span>
            <ComboboxField
              value={filters.responsibleUser}
              onChange={(value) => setFilters((current) => ({ ...current, responsibleUser: value }))}
              options={userOptions}
              placeholder="Все сотрудники"
              isClearable
            />
          </label>
        ) : null}

        <label className="field">
          <span>Создан с</span>
          <DateField
            value={filters.createdAtFrom}
            onChange={(value) => setFilters((current) => ({ ...current, createdAtFrom: value }))}
            placeholder="Любая дата"
          />
        </label>

        <label className="field">
          <span>Создан по</span>
          <DateField
            value={filters.createdAtTo}
            onChange={(value) => setFilters((current) => ({ ...current, createdAtTo: value }))}
            placeholder="Любая дата"
          />
        </label>

        <label className="field">
          <span>Сортировка</span>
          <SelectField
            value={filters.sortBy}
            onChange={(value) => setFilters((current) => ({ ...current, sortBy: (value || 'UPDATED_AT') as ProjectSortField }))}
            options={[
              { value: 'CREATED_AT', label: 'По дате создания' },
              { value: 'UPDATED_AT', label: 'По последнему обновлению' },
              { value: 'AMOUNT', label: 'По сумме' },
            ]}
          />
        </label>

        <label className="field">
          <span>Порядок</span>
          <SelectField
            value={filters.sortDirection}
            onChange={(value) =>
              setFilters((current) => ({ ...current, sortDirection: (value || 'DESC') as ProjectSortDirection }))
            }
            options={[
              { value: 'DESC', label: 'Сначала новые' },
              { value: 'ASC', label: 'Сначала старые' },
            ]}
          />
        </label>
      </FilterBar>

      <section className="panel">
        <div className="panel__header">
          <div>
            <h2 className="section-title">Список проектов</h2>
            <p className="section-hint">{projectsQuery.data.length} записей по текущему фильтру.</p>
          </div>
        </div>

        {projectsQuery.data.length ? (
          <DataTable
            columns={columns}
            rows={projectsQuery.data}
            getRowKey={(project) => project.id}
            onRowClick={(project) => navigate(`/projects/${project.id}`)}
          />
        ) : (
          <EmptyState title="Ничего не найдено" description="Попробуйте смягчить фильтр или создайте новый проект." />
        )}
      </section>
    </div>
  )
}
