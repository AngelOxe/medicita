import { forwardRef, useId } from 'react'

const Input = forwardRef(function Input({ label, error, className = '', ...props }, ref) {
  const id = useId()
  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="mb-1 block text-sm font-medium text-ink">
          {label}
        </label>
      )}
      <input
        id={id}
        ref={ref}
        aria-invalid={!!error}
        aria-describedby={error ? `${id}-error` : undefined}
        className={`w-full rounded-lg border bg-white px-3 py-2 text-sm outline-none transition-colors placeholder:text-muted focus:ring-2 ${
          error
            ? 'border-danger focus:ring-danger/30'
            : 'border-line focus:border-brand-600 focus:ring-brand-600/30'
        }`}
        {...props}
      />
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  )
})

export default Input