import { useId } from 'react'

// Lista desplegable con el mismo aspecto que Input. options = [{ value, label }]
export default function Select({ label, error, options, className = '', id, ...props }) {
  const autoId = useId()
  const selectId = id || autoId
  const errorId = `${selectId}-error`

  return (
    <div className={className}>
      {label && (
        <label htmlFor={selectId} className="mb-1.5 block text-sm font-medium text-ink">
          {label}
        </label>
      )}
      <select
        id={selectId}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? errorId : undefined}
        className={`w-full rounded-lg border bg-white py-2.5 pl-3 pr-8 text-sm outline-none transition
          focus:ring-2 focus:ring-brand-200
          ${error ? 'border-danger' : 'border-line focus:border-brand-600'}`}
        {...props}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {error && (
        <p id={errorId} className="mt-1 text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
