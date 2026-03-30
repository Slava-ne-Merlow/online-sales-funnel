export function formatDate(value?: string | null, options?: Intl.DateTimeFormatOptions) {
  if (!value) {
    return '—'
  }

  return new Intl.DateTimeFormat('ru-RU', {
    dateStyle: 'medium',
    timeStyle: options?.timeStyle ? 'short' : undefined,
    ...options,
  }).format(new Date(value))
}

export function formatDateTime(value?: string | null) {
  return formatDate(value, { timeStyle: 'short' })
}

export function formatDateOnly(value?: string | null, options?: Intl.DateTimeFormatOptions) {
  if (!value) {
    return '—'
  }

  const [year, month, day] = value.split('-').map(Number)
  return new Intl.DateTimeFormat('ru-RU', {
    dateStyle: 'long',
    ...options,
  }).format(new Date(year, (month ?? 1) - 1, day ?? 1))
}

export function formatCurrency(value?: number | null) {
  if (value === undefined || value === null) {
    return '—'
  }

  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatNumber(value?: number | null) {
  if (value === undefined || value === null) {
    return '—'
  }

  return new Intl.NumberFormat('ru-RU').format(value)
}

export function shortId(value?: string | null) {
  if (!value) {
    return '—'
  }

  return value.slice(0, 8)
}

export function toDateTimeStart(value: string) {
  return value ? `${value}T00:00:00` : undefined
}

export function toDateTimeEnd(value: string) {
  return value ? `${value}T23:59:59` : undefined
}

export function parseDateOnly(value?: string | null) {
  if (!value) {
    return null
  }

  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, (month ?? 1) - 1, day ?? 1)
}

export function serializeDateOnly(value: Date | null) {
  if (!value) {
    return ''
  }

  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
