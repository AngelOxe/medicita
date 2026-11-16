export const ROLES = { ADMIN: 'ADMIN', MEDICO: 'MEDICO', PACIENTE: 'PACIENTE' }

// Pantalla de inicio de cada rol
export const homeByRole = {
  ADMIN: '/dashboard',
  MEDICO: '/dashboard',
  PACIENTE: '/reservar',
}