import api from '../lib/axiosInstance'
import { cleanParams } from '../lib/params'

export async function listarBloqueos(params) {
  const { data } = await api.get('/bloqueos', { params: cleanParams(params) })
  return data
}

export async function crearBloqueo(payload) {
  const { data } = await api.post('/bloqueos', payload)
  return data
}

export async function actualizarBloqueo(id, payload) {
  const { data } = await api.put(`/bloqueos/${id}`, payload)
  return data
}

export async function eliminarBloqueo(id) {
  await api.delete(`/bloqueos/${id}`)
}
