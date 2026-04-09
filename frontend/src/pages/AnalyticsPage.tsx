import { useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useProjectAnalyticsQuery } from '../entities/project/queries'
import { useUsersQuery } from '../entities/user/queries'
import { useAuth } from '../features/auth/auth-context'
import type { AnalyticsPeriod, ProjectSource } from '../shared/api/types'
import { formatCurrency, formatNumber, toDateTimeEnd, toDateTimeStart } from '../shared/lib/format'
import { buildUserOptions } from '../shared/lib/project-analytics'
import {
  getProjectSourceLabel,
  getProjectStageLabel,
  getProjectStatusLabel,
  projectSourceOptions,
} from '../shared/lib/project-meta'
import { ComboboxField } from '../shared/ui/ComboboxField'
import { DateField } from '../shared/ui/DateField'
import { EmptyState, ErrorState, LoadingState } from '../shared/ui/States'
import { SegmentedControl } from '../shared/ui/SegmentedControl'
import { SelectField } from '../shared/ui/SelectField'
import { StatCard } from '../shared/ui/StatCard'

const periodOptions: Array<{ value: AnalyticsPeriod; label: string }> = [
  { value: 'LAST_WEEK', label: 'Неделя' },
  { value: 'LAST_MONTH', label: 'Месяц' },
  { value: 'LAST_YEAR', label: 'Год' },
  { value: 'ALL_TIME', label: 'Всё время' },
]

const chartPalette = ['#244a8f', '#3d6db4', '#6f8fc6', '#8ba6d1', '#c2d0e6', '#445d7a', '#8f9fb4']
const statusPalette = ['#2f6a44', '#b27a28', '#8f3f3f', '#2d5f8f', '#6a6f7a']

function formatDateInput(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function resolvePresetRange(period: AnalyticsPeriod) {
  const end = new Date()
  const start = new Date(end)

  switch (period) {
    case 'LAST_WEEK':
      start.setDate(start.getDate() - 7)
      break
    case 'LAST_MONTH':
      start.setDate(start.getDate() - 30)
      break
    case 'LAST_YEAR':
      start.setDate(start.getDate() - 365)
      break
    case 'ALL_TIME':
      return { from: '', to: '' }
  }

  return {
    from: formatDateInput(start),
    to: formatDateInput(end),
  }
}

function ChartCard({
  title,
  subtitle,
  children,
}: PropsWithChildren<{ title: string; subtitle?: string }>) {
  return (
    <section className="panel chart-card">
      <div className="panel__header">
        <div>
          <h2 className="section-title">{title}</h2>
          {subtitle ? <p className="section-hint">{subtitle}</p> : null}
        </div>
      </div>
      <div className="chart-card__body">{children}</div>
    </section>
  )
}

export function AnalyticsPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [period, setPeriod] = useState<AnalyticsPeriod>('LAST_MONTH')
  const [source, setSource] = useState<ProjectSource | ''>('')
  const [responsibleUser, setResponsibleUser] = useState('')
  const [updatedAtFrom, setUpdatedAtFrom] = useState('')
  const [updatedAtTo, setUpdatedAtTo] = useState('')
  const usersQuery = useUsersQuery(isAdmin)
  const responsibleUserOptions = useMemo(
    () =>
      buildUserOptions({
        users: usersQuery.data ?? [],
        currentUser: user,
      }),
    [user, usersQuery.data],
  )

  useEffect(() => {
    const range = resolvePresetRange(period)
    setUpdatedAtFrom(range.from)
    setUpdatedAtTo(range.to)
  }, [period])

  const analyticsQuery = useProjectAnalyticsQuery({
    period,
    source: source || undefined,
    responsibleUser: isAdmin ? responsibleUser || undefined : undefined,
    updatedAtFrom: toDateTimeStart(updatedAtFrom),
    updatedAtTo: toDateTimeEnd(updatedAtTo),
  })

  const analytics = analyticsQuery.data

  if (analyticsQuery.isPending) {
    return <LoadingState title="Загрузка аналитики" description="Собираем сводные показатели и графики." />
  }

  if (analyticsQuery.isError || !analytics) {
    return <ErrorState title="Не удалось загрузить аналитику" description="Проверьте доступность аналитического endpoint." />
  }

  const stageAmountShare = analytics.stageDistribution.map((item) => ({
    name: getProjectStageLabel(item.stage),
    value: item.amount,
    percent: item.percentOfTotalAmount,
  }))

  const statusAmountShare = analytics.statusDistribution.map((item) => ({
    name: getProjectStatusLabel(item.status),
    value: item.amount,
    percent: item.percentOfTotalAmount,
  }))

  const sourceAmountShare = analytics.sourceDistribution.map((item) => ({
    name: getProjectSourceLabel(item.source),
    value: item.amount,
    percent: item.percentOfTotalAmount,
  }))

  const stageCountBars = analytics.stageDistribution.map((item) => ({
    label: getProjectStageLabel(item.stage),
    count: item.count,
  }))

  const statusCountBars = analytics.statusDistribution.map((item) => ({
    label: getProjectStatusLabel(item.status),
    count: item.count,
  }))

  const funnelBars = analytics.funnelSummary.map((item) => ({
    label: getProjectStageLabel(item.stage),
    count: item.count,
    amount: item.amount,
  }))

  const stageAmountFunnelBars = analytics.funnelSummary.map((item) => ({
    label: getProjectStageLabel(item.stage),
    amount: item.amount,
  }))

  const statusAmountFunnelBars = analytics.statusDistribution.map((item) => ({
    label: getProjectStatusLabel(item.status),
    amount: item.amount,
  }))

  return (
    <div className="page-stack">
      <section className="panel analytics-filters">
        <div className="panel__header analytics-filters__header">
          <div>
            <h2 className="section-title">Аналитика</h2>
            <p className="section-hint">Период и диапазон дат считаются по последнему изменению проекта.</p>
          </div>
        </div>

        <div className="analytics-filters__body">
          <SegmentedControl value={period} onChange={setPeriod} options={periodOptions} />
          <div className="analytics-filters__controls">
            <label className="field">
              <span>Изменён с</span>
              <DateField value={updatedAtFrom} onChange={setUpdatedAtFrom} placeholder="Любая дата" />
            </label>
            <label className="field">
              <span>Изменён по</span>
              <DateField value={updatedAtTo} onChange={setUpdatedAtTo} placeholder="Любая дата" />
            </label>
            <label className="field">
              <span>Источник</span>
              <SelectField
                value={source}
                onChange={(value) => setSource(value as ProjectSource | '')}
                options={[{ value: '', label: 'Все источники' }, ...projectSourceOptions]}
              />
            </label>
            {isAdmin ? (
              <label className="field">
                <span>Ответственный</span>
                <ComboboxField
                  value={responsibleUser}
                  onChange={setResponsibleUser}
                  options={responsibleUserOptions}
                  placeholder="Все сотрудники"
                  isClearable
                />
              </label>
            ) : null}
          </div>
        </div>
      </section>

      <section className="stats-grid">
        <StatCard title="Всего проектов" value={formatNumber(analytics.totalProjects)} />
        <StatCard title="Общая сумма" value={formatCurrency(analytics.totalAmount)} />
        <StatCard title="В работе / отложено" value={`${formatCurrency(analytics.inProgressAmount)} / ${formatCurrency(analytics.onHoldAmount)}`} />
        <StatCard title="Не активно / проиграно / завершено" value={`${formatCurrency(analytics.inactiveAmount)} / ${formatCurrency(analytics.lostAmount)} / ${formatCurrency(analytics.completedAmount)}`} />
      </section>

      <div className="chart-grid chart-grid--triple">
        <ChartCard title="Доля суммы по этапам" subtitle="Процент от общей суммы портфеля">
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie data={stageAmountShare} dataKey="value" nameKey="name" innerRadius={64} outerRadius={96} paddingAngle={2}>
                {stageAmountShare.map((item, index) => (
                  <Cell key={item.name} fill={chartPalette[index % chartPalette.length]} />
                ))}
              </Pie>
              <Tooltip formatter={(value: number) => formatCurrency(value)} />
              <Legend verticalAlign="bottom" height={36} />
            </PieChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Доля суммы по статусам">
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie data={statusAmountShare} dataKey="value" nameKey="name" innerRadius={64} outerRadius={96} paddingAngle={2}>
                {statusAmountShare.map((item, index) => (
                  <Cell key={item.name} fill={statusPalette[index % statusPalette.length]} />
                ))}
              </Pie>
              <Tooltip formatter={(value: number) => formatCurrency(value)} />
              <Legend verticalAlign="bottom" height={36} />
            </PieChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Доля суммы по источникам">
          <div className="proportional-list">
            {sourceAmountShare.map((item, index) => (
              <div key={item.name} className="proportional-list__item">
                <div className="proportional-list__meta">
                  <span>{item.name}</span>
                  <strong>{item.percent.toFixed(1)}%</strong>
                </div>
                <div className="proportional-list__track">
                  <div
                    className="proportional-list__fill"
                    style={{
                      width: `${Math.max(item.percent, 4)}%`,
                      backgroundColor: chartPalette[index % chartPalette.length],
                    }}
                  />
                </div>
                <div className="proportional-list__amount">{formatCurrency(item.value)}</div>
              </div>
            ))}
          </div>
        </ChartCard>
      </div>

      <div className="chart-grid">
        <ChartCard title="Количество проектов по этапам">
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={stageCountBars}>
              <CartesianGrid stroke="#e8edf4" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 12 }} interval={0} angle={-12} textAnchor="end" height={58} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#244a8f" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Количество проектов по статусам">
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={statusCountBars}>
              <CartesianGrid stroke="#e8edf4" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 12 }} interval={0} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#445d7a" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <div className="chart-grid">
        <ChartCard title="Тренд создания проектов">
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart data={analytics.periodTrend}>
              <defs>
                <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#3d6db4" stopOpacity={0.28} />
                  <stop offset="100%" stopColor="#3d6db4" stopOpacity={0.02} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke="#e8edf4" vertical={false} />
              <XAxis dataKey="bucketLabel" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} />
              <Tooltip formatter={(value: number, key) => key === 'createdAmount' ? formatCurrency(value) : value} />
              <Area type="monotone" dataKey="createdCount" stroke="#244a8f" fill="url(#trendFill)" strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <div className="chart-grid chart-grid--triple">
        <ChartCard title="Воронка по этапам">
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={funnelBars} layout="vertical" margin={{ left: 24 }}>
              <CartesianGrid stroke="#e8edf4" horizontal={false} />
              <XAxis type="number" allowDecimals={false} />
              <YAxis dataKey="label" type="category" width={130} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(value: number, key) => key === 'amount' ? formatCurrency(value) : value} />
              <Bar dataKey="count" fill="#3d6db4" radius={[0, 6, 6, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Воронка по этапам по суммам">
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={stageAmountFunnelBars} layout="vertical" margin={{ left: 24 }}>
              <CartesianGrid stroke="#e8edf4" horizontal={false} />
              <XAxis type="number" />
              <YAxis dataKey="label" type="category" width={130} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(value: number) => formatCurrency(value)} />
              <Bar dataKey="amount" fill="#6f8fc6" radius={[0, 6, 6, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Воронка по статусам по суммам">
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={statusAmountFunnelBars} layout="vertical" margin={{ left: 24 }}>
              <CartesianGrid stroke="#e8edf4" horizontal={false} />
              <XAxis type="number" />
              <YAxis dataKey="label" type="category" width={130} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(value: number) => formatCurrency(value)} />
              <Bar dataKey="amount" fill="#445d7a" radius={[0, 6, 6, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <section className="panel">
        <div className="panel__header">
          <div>
            <h2 className="section-title">Топ крупных проектов</h2>
          </div>
        </div>

        {analytics.topProjects.length ? (
          <div className="ranked-list">
            {analytics.topProjects.map((project, index) => (
              <article key={project.projectId} className="ranked-list__item">
                <div className="ranked-list__index">{index + 1}</div>
                <div className="ranked-list__content">
                  <div className="table-primary">{project.title}</div>
                  <div className="table-secondary">
                    {getProjectStageLabel(project.currentStage)} · {getProjectStatusLabel(project.currentStatus)}
                    {project.responsibleUserName ? ` · ${project.responsibleUserName}` : ''}
                  </div>
                </div>
                <strong>{formatCurrency(project.amount)}</strong>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState title="Нет данных" description="Топ проектов пока пуст для выбранного периода." />
        )}
      </section>
    </div>
  )
}
