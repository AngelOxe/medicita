import {
  CalendarDays,
  CalendarPlus,
  ClipboardList,
  Clock3,
  LayoutDashboard,
  UsersRound,
} from 'lucide-react'

const dashboard = { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard }
const citas = { to: '/citas', label: 'Gestión de citas', icon: CalendarDays }
const servicios = { to: '/servicios', label: 'Servicios', icon: ClipboardList }
const horarios = { to: '/horarios', label: 'Horarios', icon: Clock3 }
const miAgenda = { to: '/citas', label: 'Mi agenda', icon: CalendarDays }
const misHorarios = { to: '/horarios', label: 'Mis horarios', icon: Clock3 }
const usuarios = { to: '/usuarios', label: 'Usuarios', icon: UsersRound }
const reservar = { to: '/reservar', label: 'Reservar cita', icon: CalendarPlus }
const misCitas = { to: '/mis-citas', label: 'Mis citas', icon: CalendarDays }

export const menuByRole = {
  PACIENTE: [reservar, misCitas],
  MEDICO: [dashboard, miAgenda, misHorarios, servicios],
  ADMIN: [dashboard, citas, servicios, horarios, usuarios],
}

export const roleLabel = {
  ADMIN: 'Administrador',
  MEDICO: 'Médico',
  PACIENTE: 'Paciente',
}

export const sectionLabel = {
  ADMIN: 'CONSULTORIO',
  MEDICO: 'CONSULTORIO',
  PACIENTE: 'PACIENTE',
}