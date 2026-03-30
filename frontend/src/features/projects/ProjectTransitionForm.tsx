import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm } from 'react-hook-form'
import { useEffect } from 'react'
import { z } from 'zod'
import type { Project, ProjectStatus, ProjectTransitionRequest } from '../../shared/api/types'
import { getProjectStatusLabel } from '../../shared/lib/project-meta'
import { Button } from '../../shared/ui/Button'
import { Modal } from '../../shared/ui/Modal'
import { SelectField } from '../../shared/ui/SelectField'

const schema = z.object({
  newStatus: z.string().min(1, 'Выберите новый статус'),
  comment: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

type ProjectTransitionFormProps = {
  open: boolean
  onClose: () => void
  project: Project
  onSubmit: (payload: ProjectTransitionRequest) => Promise<void>
  isPending?: boolean
}

export function ProjectTransitionForm({
  open,
  onClose,
  project,
  onSubmit,
  isPending,
}: ProjectTransitionFormProps) {
  const statusOptions = project.allowedStatuses
    .filter((status) => status !== project.currentStatus)
    .map((status) => ({
      value: status,
      label: getProjectStatusLabel(status),
    }))

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      newStatus: '',
      comment: '',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        newStatus: '',
        comment: '',
      })
    }
  }, [open, reset])

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Изменить статус"
      description="Статус можно менять, пока проект не переведён в успешное завершение."
    >
      <form
        className="modal-form"
        onSubmit={handleSubmit(async (values) => {
          const comment = values.comment?.trim() || undefined

          await onSubmit({
            newStatus: values.newStatus as ProjectStatus,
            comment,
          })
          onClose()
        })}
      >
        <div className="modal-summary">
          <span>Текущий статус</span>
          <strong>{getProjectStatusLabel(project.currentStatus)}</strong>
        </div>

        <label className="field field--full">
          <span>Новый статус</span>
          <Controller
            control={control}
            name="newStatus"
            render={({ field }) => (
              <SelectField
                value={field.value}
                onChange={field.onChange}
                options={statusOptions}
                placeholder="Выберите статус"
              />
            )}
          />
          {errors.newStatus ? <small>{errors.newStatus.message}</small> : null}
        </label>

        <label className="field field--full">
          <span>Комментарий</span>
          <textarea
            rows={4}
            placeholder="Необязательно"
            {...register('comment')}
          />
          {errors.comment ? <small>{errors.comment.message}</small> : null}
        </label>

        <div className="modal-actions">
          <Button variant="secondary" onClick={onClose} disabled={isPending}>
            Отмена
          </Button>
          <Button type="submit" disabled={isPending || !statusOptions.length}>
            {isPending ? 'Сохраняем...' : 'Сохранить'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
