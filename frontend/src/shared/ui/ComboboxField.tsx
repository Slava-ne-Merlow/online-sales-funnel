import { Fragment, useMemo, useState } from 'react'
import { Combobox, Transition } from '@headlessui/react'
import { cx } from '../lib/cx'
import type { UserOption } from '../lib/project-analytics'

type ComboboxFieldProps = {
  value: string
  onChange: (value: string) => void
  options: UserOption[]
  placeholder?: string
  disabled?: boolean
  isClearable?: boolean
}

export function ComboboxField({
  value,
  onChange,
  options,
  placeholder = 'Начните вводить имя',
  disabled,
  isClearable,
}: ComboboxFieldProps) {
  const [query, setQuery] = useState('')

  const filteredOptions = useMemo(() => {
    if (!query.trim()) {
      return options
    }

    const normalized = query.trim().toLowerCase()
    return options.filter((option) => {
      return option.label.toLowerCase().includes(normalized) || option.description?.toLowerCase().includes(normalized)
    })
  }, [options, query])

  const selected = options.find((option) => option.value === value) ?? null

  return (
    <Combobox value={selected} onChange={(option: UserOption | null) => onChange(option?.value ?? '')} nullable>
      <div className="select">
        <div className={cx('control', disabled && 'control--disabled')}>
          <Combobox.Input
            className="control__input"
            displayValue={(option: UserOption | null) => option?.label ?? ''}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={placeholder}
            disabled={disabled}
          />
          {isClearable && value ? (
            <button
              type="button"
              className="control__clear"
              onClick={() => {
                setQuery('')
                onChange('')
              }}
            >
              Все
            </button>
          ) : (
            <Combobox.Button className="control__chevron">▾</Combobox.Button>
          )}
        </div>

        <Transition as={Fragment} leave="transition-fade" leaveFrom="transition-open" leaveTo="transition-closed">
          <Combobox.Options className="dropdown">
            {filteredOptions.length ? (
              filteredOptions.map((option) => (
                <Combobox.Option key={option.value} value={option} className="dropdown__option">
                  {({ selected }) => (
                    <div>
                      <div className={cx('dropdown__label', selected && 'dropdown__label--selected')}>
                        {option.label}
                      </div>
                      {option.description ? <div className="dropdown__meta">{option.description}</div> : null}
                    </div>
                  )}
                </Combobox.Option>
              ))
            ) : (
              <div className="dropdown__empty">Ничего не найдено</div>
            )}
          </Combobox.Options>
        </Transition>
      </div>
    </Combobox>
  )
}
