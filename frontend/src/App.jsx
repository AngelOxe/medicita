import { Navigate, Route, Routes } from 'react-router-dom'
import HomeRedirect from './components/auth/HomeRedirect'
import ProtectedRoute from './components/auth/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'
import { ROLES } from './lib/roles'
import Forbidden from './pages/Forbidden'
import Horarios from './pages/Horarios'
import Servicios from './pages/Servicios'
import UiDemo from './pages/UiDemo'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import GestionCitas from './pages/GestionCitas'
import MisCitas from './pages/MisCitas'
import Perfil from './pages/Perfil'
import Recuperar from './pages/Recuperar'
import Register from './pages/Register'
import Reservar from './pages/Reservar'
import Restablecer from './pages/Restablecer'
import Usuarios from './pages/Usuarios'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/403" element={<Forbidden />} />
      <Route path="/ui-demo" element={<UiDemo />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/perfil" element={<Perfil />} />

          <Route element={<ProtectedRoute roles={[ROLES.PACIENTE]} />}>
            <Route path="/reservar" element={<Reservar />} />
            <Route path="/mis-citas" element={<MisCitas />} />
          </Route>

          <Route element={<ProtectedRoute roles={[ROLES.ADMIN, ROLES.MEDICO]} />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/citas" element={<GestionCitas />} />
            <Route path="/servicios" element={<Servicios />} />
            <Route path="/horarios" element={<Horarios />} />
          </Route>

          <Route element={<ProtectedRoute roles={[ROLES.ADMIN]} />}>
            <Route path="/usuarios" element={<Usuarios />} />
          </Route>
        </Route>
      </Route>
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Register />} />
      <Route path="/recuperar" element={<Recuperar />} />
      <Route path="/restablecer" element={<Restablecer />} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}