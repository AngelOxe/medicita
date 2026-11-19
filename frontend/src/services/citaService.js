import api from '../lib/axiosInstance'
import { cleanParams } from '../lib/params'

// Horarios libres de un servicio en una fecha: { slots: [{ inicio, fin }] }
export async function obtenerDisponibilidad(servicioId, fecha) {
  const { data } = await api.get('/disponibilidad', { params: { servicioId, fecha } })
  return data
}

export async function reservarCita(payload) {
  const { data } = await api.post('/citas', payload)
  return data
}

// Citas del paciente que inició sesión. tipo = 'proximas' | 'historial'
export async function listarMisCitas(params) {
  const { data } = await api.get('/citas/mias', { params: cleanParams(params) })
  return data
}

// Agenda completa (ADMIN y MEDICO) con filtros: search, estado, fecha
export async function listarCitas(params) {
  const { data } = await api.get('/citas', { params: cleanParams(params) })
  return data
}

export async function cancelarCita(id) {
  const { data } = await api.patch(`/citas/${id}/cancelar`)
  return data
}

// estado = 'completada' | 'no_asistio'
export async function cambiarEstadoCita(id, estado) {
  const { data } = await api.patch(`/citas/${id}/estado`, { estado })
  return data
}
