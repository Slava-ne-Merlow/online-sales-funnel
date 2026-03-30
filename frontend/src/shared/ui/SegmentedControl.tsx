import { cx } from '../lib/cx'

type Option<T extends string> = {
  value: T
  label: string
}

type SegmentedControlProps<T extends string> = {
  value: T
  onChange: (value: T) => void
  options: Array<Option<T>>
}

export function SegmentedControl<T extends string>({
  value,
  onChange,
  options,
}: SegmentedControlProps<T>) {
  return (
    <div className="segmented-control" role="tablist" aria-label="Период аналитики">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={cx('segmented-control__item', value === option.value && 'segmented-control__item--active')}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
