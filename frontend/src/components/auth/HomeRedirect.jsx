import { Navigate } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'
import { homeByRole } from '../../lib/roles'

export default function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user ? homeByRole[user.rol] : '/login'} replace />
}