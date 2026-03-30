import type { ReactNode } from 'react'
import { cx } from '../lib/cx'

export type DataTableColumn<T> = {
  key: string
  header: string
  cell: (row: T) => ReactNode
  className?: string
}

type DataTableProps<T> = {
  columns: Array<DataTableColumn<T>>
  rows: T[]
  getRowKey: (row: T) => string
  onRowClick?: (row: T) => void
}

export function DataTable<T>({ columns, rows, getRowKey, onRowClick }: DataTableProps<T>) {
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key}>{column.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={getRowKey(row)}
              className={cx(onRowClick && 'data-table__row--interactive')}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
            >
              {columns.map((column) => (
                <td key={column.key} className={column.className}>
                  {column.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
