import { Navigate, Route, Routes } from 'react-router-dom'
import HomeRedirect from './components/auth/HomeRedirect'
import ProtectedRoute from './components/auth/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'
import { ROLES } from './lib/roles'
import Forbidden from './pages/Forbidden'
import Pendiente from './pages/Pendiente'
import UiDemo from './pages/UiDemo'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/403" element={<Forbidden />} />
      <Route path="/ui-demo" element={<UiDemo />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/perfil" element={<Pendiente titulo="Perfil" />} />

          <Route element={<ProtectedRoute roles={[ROLES.PACIENTE]} />}>
            <Route path="/reservar" element={<Pendiente titulo="Reservar cita" />} />
            <Route path="/mis-citas" element={<Pendiente titulo="Mis citas" />} />
          </Route>

          <Route element={<ProtectedRoute roles={[ROLES.ADMIN, ROLES.MEDICO]} />}>
            <Route path="/dashboard" element={<Pendiente titulo="Dashboard" />} />
            <Route path="/citas" element={<Pendiente titulo="Gestión de citas" />} />
            <Route path="/servicios" element={<Pendiente titulo="Servicios" />} />
            <Route path="/horarios" element={<Pendiente titulo="Horarios" />} />
          </Route>

          <Route element={<ProtectedRoute roles={[ROLES.ADMIN]} />}>
            <Route path="/usuarios" element={<Pendiente titulo="Usuarios" />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}