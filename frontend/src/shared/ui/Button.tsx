import type { ButtonHTMLAttributes, PropsWithChildren } from 'react'
import { cx } from '../lib/cx'

type ButtonProps = PropsWithChildren<
  ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  }
>

export function Button({ children, className, variant = 'primary', type = 'button', ...props }: ButtonProps) {
  return (
    <button
      type={type}
      className={cx('button', `button--${variant}`, className)}
      {...props}
    >
      {children}
    </button>
  )
}
