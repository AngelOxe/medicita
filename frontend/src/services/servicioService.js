import api from '../lib/axiosInstance'
import { cleanParams } from '../lib/params'

export async function listarServicios(params) {
  const { data } = await api.get('/servicios', { params: cleanParams(params) })
  return data
}

export async function crearServicio(payload) {
  const { data } = await api.post('/servicios', payload)
  return data
}

export async function actualizarServicio(id, payload) {
  const { data } = await api.put(`/servicios/${id}`, payload)
  return data
}

// Borrado lógico: el servicio queda inactivo (204)
export async function desactivarServicio(id) {
  await api.delete(`/servicios/${id}`)
}
