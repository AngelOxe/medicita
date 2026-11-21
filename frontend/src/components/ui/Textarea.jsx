import { useId } from 'react'

export default function Textarea({ label, error, className = '', id, ...props }) {
  const autoId = useId()
  const areaId = id || autoId
  const errorId = `${areaId}-error`

  return (
    <div className={className}>
      {label && (
        <label htmlFor={areaId} className="mb-1.5 block text-sm font-medium text-ink">
          {label}
        </label>
      )}
      <textarea
        id={areaId}
        rows={3}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? errorId : undefined}
        className={`w-full rounded-lg border bg-white px-3 py-2.5 text-sm outline-none transition
          focus:ring-2 focus:ring-brand-200
          ${error ? 'border-danger' : 'border-line focus:border-brand-600'}`}
        {...props}
      />
      {error && (
        <p id={errorId} className="mt-1 text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
