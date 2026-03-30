import { env } from '../config/env'
import { getStoredToken, removeStoredToken } from '../lib/auth-storage'
import type { StatusResponse } from './types'

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH'
  query?: Record<string, string | number | boolean | undefined | null>
  body?: unknown
  auth?: boolean
}

export class ApiError extends Error {
  status: number
  data?: unknown

  constructor(message: string, status: number, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

function buildUrl(path: string, query?: RequestOptions['query']) {
  const url = new URL(path, env.apiUrl)

  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') {
        return
      }

      url.searchParams.set(key, String(value))
    })
  }

  return url.toString()
}

function getErrorMessage(status: number, data: unknown) {
  if (typeof data === 'object' && data && 'message' in data) {
    return String((data as StatusResponse).message)
  }

  if (status === 401) {
    return 'Сессия истекла. Войдите снова.'
  }

  return 'Не удалось выполнить запрос.'
}

export async function request<T>(path: string, options: RequestOptions = {}) {
  const headers = new Headers()
  const token = getStoredToken()

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.auth !== false && token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(buildUrl(path, options.query), {
    method: options.method ?? 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  const contentType = response.headers.get('content-type')
  const hasJson = contentType?.includes('application/json')
  const data = hasJson ? await response.json() : null

  if (!response.ok) {
    if (response.status === 401) {
      removeStoredToken()
      window.dispatchEvent(new Event('app:unauthorized'))
    }

    throw new ApiError(getErrorMessage(response.status, data), response.status, data)
  }

  return data as T
}
