import { ShieldAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { homeByRole } from '../lib/roles'

export default function Forbidden() {
  const { user } = useAuth()
  const destino = user ? homeByRole[user.rol] : '/login'

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-8 text-center">
      <ShieldAlert className="h-12 w-12 text-danger" aria-hidden="true" />
      <h1 className="text-2xl font-bold text-ink">403 · Acceso denegado</h1>
      <p className="max-w-md text-sm text-muted">
        No tienes permiso para ver esta página con tu rol actual.
      </p>
      <Link
        to={destino}
        className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
      >
        Volver al inicio
      </Link>
    </div>
  )
}
