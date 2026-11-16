import api from '../lib/axiosInstance'

export async function login(email, password) {
  const { data } = await api.post('/auth/login', { email, password })
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