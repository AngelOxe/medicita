import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'
import Spinner from '../ui/Spinner'

export default function ProtectedRoute({ roles }) {
  const { user, isAuthenticated, checking } = useAuth()
  const location = useLocation()

  if (checking) {
    return (
      <div className="flex min-h-screen items-center justify-center text-brand-600">
        <Spinner />
      </div>
    )
  }
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />
  if (roles && !roles.includes(user.rol)) return <Navigate to="/403" replace />
  return <Outlet />
}