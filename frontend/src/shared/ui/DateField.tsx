import { forwardRef } from 'react'
import DatePicker, { registerLocale } from 'react-datepicker'
import { ru } from 'date-fns/locale/ru'
import 'react-datepicker/dist/react-datepicker.css'
import { formatDateOnly, parseDateOnly, serializeDateOnly } from '../lib/format'

registerLocale('ru', ru)

type DateFieldProps = {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}

type TriggerProps = {
  value?: string
  onClick?: () => void
  placeholder?: string
}

const DateFieldTrigger = forwardRef<HTMLButtonElement, TriggerProps>(function DateFieldTrigger(
  { value, onClick, placeholder },
  ref,
) {
  return (
    <button ref={ref} type="button" className="control control--button" onClick={onClick}>
      <span>{value || placeholder}</span>
      <span className="control__chevron">▾</span>
    </button>
  )
})

export function DateField({ value, onChange, placeholder = 'Выберите дату' }: DateFieldProps) {
  const selected = parseDateOnly(value)
  const displayValue = value ? formatDateOnly(value) : ''

  return (
    <div className="date-field">
      <DatePicker
        selected={selected}
        onChange={(date) => onChange(serializeDateOnly(date))}
        locale="ru"
        dateFormat="dd.MM.yyyy"
        placeholderText={placeholder}
        isClearable
        className="control__input"
        wrapperClassName="date-field__wrapper"
        popperClassName="date-field__popper"
        calendarClassName="date-field__calendar"
        customInput={<DateFieldTrigger value={displayValue} placeholder={placeholder} />}
      />
    </div>
  )
}
