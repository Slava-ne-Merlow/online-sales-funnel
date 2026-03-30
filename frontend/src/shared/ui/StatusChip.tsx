import { cx } from '../lib/cx'

type StatusChipProps = {
  label: string
  tone?: 'neutral' | 'info' | 'success' | 'warning' | 'danger'
}

export function StatusChip({ label, tone = 'neutral' }: StatusChipProps) {
  return <span className={cx('chip', `chip--${tone}`)}>{label}</span>
}
