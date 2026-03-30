import type { PropsWithChildren } from 'react'
import { Dialog, DialogBackdrop, DialogPanel } from '@headlessui/react'

type ModalProps = PropsWithChildren<{
  open: boolean
  onClose: () => void
  title: string
  description?: string
}>

export function Modal({ open, onClose, title, description, children }: ModalProps) {
  return (
    <Dialog open={open} onClose={onClose} className="modal-root">
      <DialogBackdrop className="modal-backdrop" />
      <div className="modal-layout">
        <DialogPanel className="modal-panel">
          <div className="modal-panel__header">
            <div>
              <h2>{title}</h2>
              {description ? <p>{description}</p> : null}
            </div>
            <button type="button" className="icon-button" onClick={onClose} aria-label="Закрыть окно">
              ×
            </button>
          </div>
          {children}
        </DialogPanel>
      </div>
    </Dialog>
  )
}
