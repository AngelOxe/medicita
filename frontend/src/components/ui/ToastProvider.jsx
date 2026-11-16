import { useCallback, useState } from 'react'
import { ToastContext } from './toast-context'

const styles = {
  success: 'border-ok/30 bg-ok-bg text-ok',
  error: 'border-danger/30 bg-danger-bg text-danger',
  info: 'border-brand-200 bg-brand-50 text-brand-700',
}

export default function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const show = useCallback((message, type = 'info') => {
    const id = crypto.randomUUID()
    setToasts((list) => [...list, { id, message, type }])
    setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), 4000)
  }, [])

  return (
    <ToastContext.Provider value={{ show }}>
      {children}
      <div className="fixed right-4 top-4 z-50 flex flex-col gap-2" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`rounded-lg border px-4 py-3 text-sm shadow-md ${styles[t.type]}`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}