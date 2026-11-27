import api from '../lib/axiosInstance'

export async function login(email, password) {
  const { data } = await api.post('/auth/login', { email, password })
  return data
}

export async function loginConGoogle(credential) {
  const { data } = await api.post('/auth/google', { credential })
  return data
}

// Pública: dice si el backend tiene configurado el inicio de sesión con Google
export async function obtenerConfigGoogle() {
  const { data } = await api.get('/auth/google/config')
  return data
}

export async function register(payload) {
  const { data } = await api.post('/auth/register', payload)
  return data
}

export async function logoutRequest() {
  await api.post('/auth/logout')
}

export async function getPerfil() {
  const { data } = await api.get('/perfil')
  return data
}

export async function solicitarRecuperacion(email) {
  const { data } = await api.post('/auth/forgot-password', { email })
  return data
}

export async function restablecerPassword(token, password) {
  const { data } = await api.post('/auth/reset-password', { token, password })
  return data
}
