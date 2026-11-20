import api from '../lib/axiosInstance'
import { cleanParams } from '../lib/params'

export async function listarUsuarios(params) {
  const { data } = await api.get('/usuarios', { params: cleanParams(params) })
  return data
}

export async function crearUsuario(payload) {
  const { data } = await api.post('/usuarios', payload)
  return data
}

// Solo nombre, teléfono y activo (el correo, el rol y la contraseña no cambian aquí)
export async function actualizarUsuario(id, payload) {
  const { data } = await api.put(`/usuarios/${id}`, payload)
  return data
}

// Borrado lógico: la cuenta queda inactiva (204)
export async function desactivarUsuario(id) {
  await api.delete(`/usuarios/${id}`)
}

// Borrado definitivo: solo cuentas sin citas ni historial (si no, el servidor responde 422)
export async function eliminarUsuario(id) {
  await api.delete(`/usuarios/${id}`, { params: { definitivo: true } })
}

export async function actualizarPerfil(payload) {
  const { data } = await api.put('/perfil', payload)
  return data
}

export async function obtenerResumen() {
  const { data } = await api.get('/dashboard/resumen')
  return data
}
