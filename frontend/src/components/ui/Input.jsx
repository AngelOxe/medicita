import { forwardRef, useId } from 'react'

const Input = forwardRef(function Input(
  { label, error, icon: Icon, endAdornment, className = '', id, ...props },
  ref,
) {
  const autoId = useId()
  const inputId = id || autoId
  const errorId = `${inputId}-error`

  return (
    <div className={className}>
      {label && (
        <label htmlFor={inputId} className="mb-1.5 block text-sm font-medium text-ink">
          {label}
        </label>
      )}
      <div className="relative">
        {Icon && (
          <Icon
            size={18}
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted"
          />
        )}
        <input
          ref={ref}
          id={inputId}
          aria-invalid={error ? 'true' : 'false'}
          aria-describedby={error ? errorId : undefined}
          className={`w-full rounded-lg border bg-white py-2.5 text-sm outline-none transition
            focus:ring-2 focus:ring-brand-200
            ${Icon ? 'pl-10' : 'pl-3'} ${endAdornment ? 'pr-11' : 'pr-3'}
            ${error ? 'border-danger' : 'border-line focus:border-brand-600'}`}
          {...props}
        />
        {endAdornment && (
          <div className="absolute right-2 top-1/2 -translate-y-1/2">{endAdornment}</div>
        )}
      </div>
      {error && (
        <p id={errorId} className="mt-1 text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  )
})

export default Input