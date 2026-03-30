import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { projectSourceOptions } from '../../shared/lib/project-meta'
import { Button } from '../../shared/ui/Button'
import { ComboboxField } from '../../shared/ui/ComboboxField'
import { SelectField } from '../../shared/ui/SelectField'
import type { CreateProjectRequest } from '../../shared/api/types'
import type { UserOption } from '../../shared/lib/project-analytics'

const schema = z.object({
  title: z.string().trim().min(1, 'Укажите название проекта'),
  source: z.enum(['TENDER', 'DIRECT_SALES', 'WEBSITE', 'GK_OSTEK']),
  initialAmount: z.string().optional(),
  globalComment: z.string().optional(),
  responsibleUserId: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

type ProjectFormProps = {
  onSubmit: (payload: CreateProjectRequest) => Promise<void>
  isPending?: boolean
  submitLabel?: string
  showResponsibleUser: boolean
  responsibleUserOptions: UserOption[]
}

export function ProjectForm({
  onSubmit,
  isPending,
  submitLabel = 'Создать проект',
  showResponsibleUser,
  responsibleUserOptions,
}: ProjectFormProps) {
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: '',
      source: 'TENDER',
      initialAmount: '',
      globalComment: '',
      responsibleUserId: '',
    },
  })

  return (
    <form
      className="form form--compact"
      onSubmit={handleSubmit(async (values) => {
        await onSubmit({
          title: values.title.trim(),
          source: values.source,
          initialAmount: values.initialAmount ? Number(values.initialAmount) : undefined,
          globalComment: values.globalComment?.trim() || undefined,
          responsibleUserId: showResponsibleUser ? values.responsibleUserId?.trim() || undefined : undefined,
        })
      })}
    >
      <label className="field">
        <span>Название проекта</span>
        <input placeholder="Например, модернизация линии закалки" {...register('title')} />
        {errors.title ? <small>{errors.title.message}</small> : null}
      </label>

      <label className="field">
        <span>Источник</span>
        <Controller
          control={control}
          name="source"
          render={({ field }) => (
            <SelectField
              value={field.value}
              onChange={field.onChange}
              options={projectSourceOptions}
              placeholder="Выберите источник"
            />
          )}
        />
      </label>

      <label className="field">
        <span>Начальная сумма</span>
        <input type="number" min="0" step="1" placeholder="Например, 2500000" {...register('initialAmount')} />
      </label>

      {showResponsibleUser ? (
        <label className="field">
          <span>Ответственный</span>
          <Controller
            control={control}
            name="responsibleUserId"
            render={({ field }) => (
              <ComboboxField
                value={field.value ?? ''}
                onChange={field.onChange}
                options={responsibleUserOptions}
                placeholder="Выберите сотрудника"
              />
            )}
          />
        </label>
      ) : null}

      <label className="field field--full">
        <span>Комментарий</span>
        <textarea rows={5} placeholder="Краткий контекст, вводные или договорённости" {...register('globalComment')} />
      </label>

      <div className="form__footer">
        <Button type="submit" disabled={isPending}>
          {isPending ? 'Сохраняем...' : submitLabel}
        </Button>
      </div>
    </form>
  )
}
