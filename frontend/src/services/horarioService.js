import api from '../lib/axiosInstance'
import { cleanParams } from '../lib/params'

export async function listarHorarios(params) {
  const { data } = await api.get('/horarios', { params: cleanParams(params) })
  return data
}

export async function crearHorario(payload) {
  const { data } = await api.post('/horarios', payload)
  return data
}

export async function actualizarHorario(id, payload) {
  const { data } = await api.put(`/horarios/${id}`, payload)
  return data
}

// Borrado definitivo del horario (204). Si dejara citas futuras sin cobertura, el servidor responde 422
export async function eliminarHorario(id) {
  await api.delete(`/horarios/${id}`, { params: { definitivo: true } })
}

// Borrado lógico: el horario queda inactivo (204)
export async function desactivarHorario(id) {
  await api.delete(`/horarios/${id}`)
}
