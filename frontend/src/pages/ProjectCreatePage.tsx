import { useMemo, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useUsersQuery } from '../entities/user/queries'
import { useAuth } from '../features/auth/auth-context'
import { ProjectForm } from '../features/projects/ProjectForm'
import { queryClient } from '../shared/api/query-client'
import { projectsApi } from '../shared/api/services'
import { buildUserOptions } from '../shared/lib/project-analytics'
import { Button } from '../shared/ui/Button'

export function ProjectCreatePage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const showResponsibleUser = user?.role === 'ADMIN'
  const usersQuery = useUsersQuery(showResponsibleUser)

  const responsibleUserOptions = useMemo(
    () =>
      buildUserOptions({
        users: usersQuery.data ?? [],
        currentUser: user,
      }),
    [user, usersQuery.data],
  )

  const createMutation = useMutation({
    mutationFn: projectsApi.createProject,
    onSuccess: async (project) => {
      await queryClient.invalidateQueries({ queryKey: ['projects'] })
      navigate(`/projects/${project.id}`)
    },
  })

  return (
    <div className="page-stack">
      <section className="panel">
        <div className="panel__header">
          <div>
            <h2 className="section-title">Новый проект</h2>
            <p className="section-hint">
              {showResponsibleUser
                ? 'Для администратора ответственного можно выбрать из списка сотрудников.'
                : 'Ответственный будет назначен автоматически на текущего пользователя.'}
            </p>
          </div>
          <Button variant="secondary" onClick={() => navigate('/projects')}>
            Назад к списку
          </Button>
        </div>

        {error ? <div className="alert alert--error">{error}</div> : null}

        <ProjectForm
          showResponsibleUser={showResponsibleUser}
          responsibleUserOptions={responsibleUserOptions}
          isPending={createMutation.isPending}
          onSubmit={async (payload) => {
            try {
              setError(null)
              await createMutation.mutateAsync(payload)
            } catch (submissionError) {
              setError(submissionError instanceof Error ? submissionError.message : 'Не удалось создать проект.')
            }
          }}
        />
      </section>
    </div>
  )
}
