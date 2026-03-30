import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useMemo, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { useProjectHistoryQuery, useProjectQuery } from '../entities/project/queries'
import { ProjectTransitionForm } from '../features/projects/ProjectTransitionForm'
import { queryClient } from '../shared/api/query-client'
import { projectsApi } from '../shared/api/services'
import { formatCurrency, formatDateTime } from '../shared/lib/format'
import { buildTimelineItems } from '../shared/lib/project-history'
import {
  getProjectSourceLabel,
  getProjectStageLabel,
  getProjectStatusLabel,
} from '../shared/lib/project-meta'
import { Button } from '../shared/ui/Button'
import { EmptyState, ErrorState, LoadingState } from '../shared/ui/States'
import { StatusChip } from '../shared/ui/StatusChip'
import { Timeline } from '../shared/ui/Timeline'

const editSchema = z.object({
  currentAmount: z.string().optional(),
  globalComment: z.string().optional(),
})

type EditFormValues = z.infer<typeof editSchema>

function getStatusTone(status: string) {
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

export function ProjectDetailsPage() {
  const navigate = useNavigate()
  const { projectId } = useParams<{ projectId: string }>()
  const [editError, setEditError] = useState<string | null>(null)
  const [stageError, setStageError] = useState<string | null>(null)
  const [statusError, setStatusError] = useState<string | null>(null)
  const [showStatusModal, setShowStatusModal] = useState(false)
  const [showAdvanceComment, setShowAdvanceComment] = useState(false)
  const [advanceComment, setAdvanceComment] = useState('')
  const projectQuery = useProjectQuery(projectId)
  const historyQuery = useProjectHistoryQuery(projectId)

  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = useForm<EditFormValues>({
    resolver: zodResolver(editSchema),
    defaultValues: {
      currentAmount: '',
      globalComment: '',
    },
  })

  useEffect(() => {
    if (projectQuery.data) {
      reset({
        currentAmount: projectQuery.data.currentAmount?.toString() ?? '',
        globalComment: projectQuery.data.globalComment ?? '',
      })
    }
  }, [projectQuery.data, reset])

  const invalidateProject = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['projects'] }),
      queryClient.invalidateQueries({ queryKey: ['projects', 'detail', projectId] }),
      queryClient.invalidateQueries({ queryKey: ['projects', 'history', projectId] }),
    ])
  }

  const editMutation = useMutation({
    mutationFn: (values: { currentAmount?: number; globalComment?: string }) =>
      projectsApi.updateProject(projectId!, values),
    onSuccess: invalidateProject,
  })

  const advanceMutation = useMutation({
    mutationFn: (comment?: string) =>
      projectsApi.advanceProject(projectId!, { comment: comment?.trim() || undefined }),
    onSuccess: async () => {
      setAdvanceComment('')
      setShowAdvanceComment(false)
      await invalidateProject()
    },
  })

  const statusMutation = useMutation({
    mutationFn: (payload: Parameters<typeof projectsApi.transitionProject>[1]) =>
      projectsApi.transitionProject(projectId!, payload),
    onSuccess: invalidateProject,
  })

  const timelineItems = useMemo(() => buildTimelineItems(historyQuery.data ?? [], []), [historyQuery.data])

  if (projectQuery.isPending || historyQuery.isPending) {
    return <LoadingState title="Загрузка проекта" description="Получаем актуальную карточку и историю изменений." />
  }

  if (projectQuery.isError || historyQuery.isError) {
    return <ErrorState title="Не удалось открыть проект" description="Попробуйте обновить страницу или вернуться к списку." />
  }

  if (!projectQuery.data) {
    return <EmptyState title="Проект не найден" description="Вернитесь в список и выберите существующий проект." />
  }

  const project = projectQuery.data

  return (
    <div className="page-stack">
      <section className="panel">
        <div className="panel__header">
          <div>
            <div className="section-overline">Карточка проекта</div>
            <h2 className="section-title">{project.title}</h2>
            <p className="section-hint">
              Обновлён {formatDateTime(project.updatedAt)}
            </p>
          </div>
          <Button variant="secondary" onClick={() => navigate('/projects')}>
            К списку
          </Button>
        </div>

        <div className="details-grid details-grid--wide">
          <div className="details-card">
            <span>Источник</span>
            <strong>{getProjectSourceLabel(project.source)}</strong>
          </div>
          <div className="details-card">
            <span>Этап</span>
            <strong>{getProjectStageLabel(project.currentStage)}</strong>
          </div>
          <div className="details-card">
            <span>Статус</span>
            <strong>{getProjectStatusLabel(project.currentStatus)}</strong>
          </div>
          <div className="details-card">
            <span>Начальная сумма</span>
            <strong>{formatCurrency(project.initialAmount)}</strong>
          </div>
          <div className="details-card">
            <span>Текущая сумма</span>
            <strong>{formatCurrency(project.currentAmount)}</strong>
          </div>
          <div className="details-card">
            <span>Ответственный</span>
            <strong>{project.responsibleUserName || 'Не назначен'}</strong>
          </div>
          <div className="details-card">
            <span>Создал</span>
            <strong>{project.createdByName || 'Сотрудник компании'}</strong>
          </div>
          <div className="details-card">
            <span>Дата создания</span>
            <strong>{formatDateTime(project.createdAt)}</strong>
          </div>
          <div className="details-card">
            <span>Последнее обновление</span>
            <strong>{formatDateTime(project.updatedAt)}</strong>
          </div>
        </div>
      </section>

      <div className="content-grid">
        <section className="panel">
          <div className="panel__header">
            <div>
              <h2 className="section-title">Основная информация</h2>
              <p className="section-hint">Здесь можно обновить текущую сумму и общий комментарий.</p>
            </div>
          </div>

          {editError ? <div className="alert alert--error">{editError}</div> : null}

          <form
            className="form form--compact"
            onSubmit={handleSubmit(async (values) => {
              try {
                setEditError(null)
                await editMutation.mutateAsync({
                  currentAmount: values.currentAmount ? Number(values.currentAmount) : undefined,
                  globalComment: values.globalComment?.trim() || undefined,
                })
              } catch (submissionError) {
                setEditError(submissionError instanceof Error ? submissionError.message : 'Не удалось сохранить изменения.')
              }
            })}
          >
            <label className="field">
              <span>Текущая сумма</span>
              <input type="number" min="0" step="1" {...register('currentAmount')} />
            </label>

            <label className="field field--full">
              <span>Комментарий</span>
              <textarea rows={8} {...register('globalComment')} />
            </label>

            <div className="form__footer">
              <Button type="submit" disabled={isSubmitting || editMutation.isPending}>
                {isSubmitting || editMutation.isPending ? 'Сохраняем...' : 'Сохранить изменения'}
              </Button>
            </div>
          </form>
        </section>

        <div className="page-stack">
          <section className="panel">
            <div className="panel__header">
              <div>
                <h2 className="section-title">Этап</h2>
                <p className="section-hint">Обычное продвижение по воронке вынесено в отдельное простое действие.</p>
              </div>
            </div>

            {stageError ? <div className="alert alert--error">{stageError}</div> : null}

            <div className="panel__body">
              <div className="inline-metrics">
                <div>
                  <span>Текущий этап</span>
                  <strong>{getProjectStageLabel(project.currentStage)}</strong>
                </div>
                <div>
                  <span>Следующий этап</span>
                  <strong>{project.nextStage ? getProjectStageLabel(project.nextStage) : 'Не определён'}</strong>
                </div>
              </div>

              {project.canAdvanceStage && project.nextStage ? (
                <div className="stack-sm">
                  {showAdvanceComment ? (
                    <label className="field field--full">
                      <span>Комментарий к переходу</span>
                      <textarea
                        rows={3}
                        placeholder="Необязательно"
                        value={advanceComment}
                        onChange={(event) => setAdvanceComment(event.target.value)}
                      />
                    </label>
                  ) : null}

                  <div className="inline-actions">
                    <Button
                      onClick={async () => {
                        try {
                          setStageError(null)
                          await advanceMutation.mutateAsync(advanceComment)
                        } catch (submissionError) {
                          setStageError(submissionError instanceof Error ? submissionError.message : 'Не удалось продвинуть проект.')
                        }
                      }}
                      disabled={advanceMutation.isPending}
                    >
                      {advanceMutation.isPending ? 'Продвигаем...' : 'Следующий этап'}
                    </Button>
                    <Button variant="ghost" onClick={() => setShowAdvanceComment((current) => !current)}>
                      {showAdvanceComment ? 'Скрыть комментарий' : 'Добавить комментарий'}
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="info-block">
                  Дальнейшее продвижение по этапам доступно только для проектов со статусом «В работе».
                </div>
              )}
            </div>
          </section>

          <section className="panel">
            <div className="panel__header">
              <div>
                <h2 className="section-title">Статус</h2>
                <p className="section-hint">После статуса «Успешна / завершена» дальнейшая смена статуса заблокирована.</p>
              </div>
            </div>

            {statusError ? <div className="alert alert--error">{statusError}</div> : null}

            <div className="panel__body">
              <div className="inline-metrics">
                <div>
                  <span>Текущий статус</span>
                  <strong>
                    <StatusChip label={getProjectStatusLabel(project.currentStatus)} tone={getStatusTone(project.currentStatus)} />
                  </strong>
                </div>
              </div>

              {project.currentStatus === 'DONE' ? (
                <div className="info-block info-block--warning">
                  Проект переведён в успешное завершение. Изменение статуса больше недоступно.
                </div>
              ) : null}

              <div className="stack-sm">
                <div className="inline-actions">
                  <Button
                    variant="secondary"
                    onClick={() => setShowStatusModal(true)}
                    disabled={!project.canChangeStatus || project.allowedStatuses.length === 0}
                  >
                    Изменить статус
                  </Button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <section className="panel">
        <div className="panel__header">
          <div>
            <h2 className="section-title">История</h2>
            <p className="section-hint">Все изменения проекта собраны в читаемую ленту по данным backend.</p>
          </div>
        </div>

        {timelineItems.length ? (
          <Timeline items={timelineItems} />
        ) : (
          <EmptyState title="История пока пуста" description="По этому проекту ещё не было значимых изменений." />
        )}
      </section>

      <ProjectTransitionForm
        open={showStatusModal}
        onClose={() => setShowStatusModal(false)}
        project={project}
        isPending={statusMutation.isPending}
        onSubmit={async (payload) => {
          try {
            setStatusError(null)
            await statusMutation.mutateAsync(payload)
          } catch (submissionError) {
            setStatusError(submissionError instanceof Error ? submissionError.message : 'Не удалось изменить статус.')
          }
        }}
      />
    </div>
  )
}
