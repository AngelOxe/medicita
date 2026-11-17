import axios from 'axios'

export const TOKEN_KEY = 'medicita_token'
export const USER_KEY = 'medicita_user'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8085/api',
  timeout: 15000,
})

// Envía el token JWT en cada petición protegida
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      // Sin respuesta: servidor caído o sin internet
      error.userMessage =
        'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.'
    } else if (error.response.status === 401 && !error.config.url.includes('/auth/login')) {
      // Token vencido o revocado: cerrar sesión
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      window.dispatchEvent(new Event('auth:logout'))
    }
    return Promise.reject(error)
  },
)

export default api