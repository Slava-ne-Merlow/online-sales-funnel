import { Fragment } from 'react'
import { Listbox, Transition } from '@headlessui/react'
import { cx } from '../lib/cx'

export type SelectOption<T extends string> = {
  value: T
  label: string
  description?: string
}

type SelectFieldProps<T extends string> = {
  value: T | ''
  onChange: (value: T | '') => void
  options: Array<SelectOption<T>>
  placeholder?: string
  disabled?: boolean
}

export function SelectField<T extends string>({
  value,
  onChange,
  options,
  placeholder = 'Выберите значение',
  disabled,
}: SelectFieldProps<T>) {
  const selected = options.find((option) => option.value === value) ?? null

  return (
    <Listbox value={selected} onChange={(option) => onChange(option?.value ?? '')} disabled={disabled}>
      <div className="select">
        <Listbox.Button className={cx('control', 'control--button', disabled && 'control--disabled')}>
          <span className={cx(!selected && 'control__placeholder')}>{selected?.label ?? placeholder}</span>
          <span className="control__chevron">▾</span>
        </Listbox.Button>
        <Transition
          as={Fragment}
          leave="transition-fade"
          leaveFrom="transition-open"
          leaveTo="transition-closed"
        >
          <Listbox.Options className="dropdown">
            {options.map((option) => (
              <Listbox.Option key={option.value} value={option} className="dropdown__option">
                {({ selected }) => (
                  <div>
                    <div className={cx('dropdown__label', selected && 'dropdown__label--selected')}>
                      {option.label}
                    </div>
                    {option.description ? <div className="dropdown__meta">{option.description}</div> : null}
                  </div>
                )}
              </Listbox.Option>
            ))}
          </Listbox.Options>
        </Transition>
      </div>
    </Listbox>
  )
}
