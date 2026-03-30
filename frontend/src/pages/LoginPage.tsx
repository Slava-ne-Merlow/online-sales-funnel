import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '../features/auth/auth-context'
import { ApiError } from '../shared/api/http'
import { Button } from '../shared/ui/Button'

const schema = z.object({
  email: z.email('Укажите корректный email'),
  password: z.string().min(8, 'Пароль должен содержать минимум 8 символов'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { signIn } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  return (
    <div className="login-layout">
      <section className="login-panel">
        <div className="login-panel__header">
          <p className="login-panel__eyebrow">Воронка продаж</p>
          <h1>Вход в CRM</h1>
          <p>Используйте рабочую учетную запись, чтобы продолжить работу с проектами и аналитикой.</p>
        </div>

        <form
          className="login-form"
          onSubmit={handleSubmit(async (values) => {
            try {
              setError(null)
              await signIn(values)
              navigate(from, { replace: true })
            } catch (submissionError) {
              setError(
                submissionError instanceof ApiError ? submissionError.message : 'Не удалось выполнить вход.',
              )
            }
          })}
        >
          <label className="field field--full">
            <span>Email</span>
            <input type="email" autoComplete="username" placeholder="name@company.ru" {...register('email')} />
            {errors.email ? <small>{errors.email.message}</small> : null}
          </label>

          <label className="field field--full">
            <span>Пароль</span>
            <div className="password-field">
              <input
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="Введите пароль"
                {...register('password')}
              />
              <button
                type="button"
                className="password-field__toggle"
                onClick={() => setShowPassword((current) => !current)}
              >
                {showPassword ? 'Скрыть' : 'Показать'}
              </button>
            </div>
            {errors.password ? <small>{errors.password.message}</small> : null}
          </label>

          {error ? <div className="alert alert--error">{error}</div> : null}

          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Входим...' : 'Войти'}
          </Button>
        </form>
      </section>
    </div>
  )
}
