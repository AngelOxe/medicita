import { useCallback, useEffect, useMemo, useState } from 'react'
import { TOKEN_KEY, USER_KEY } from '../lib/axiosInstance'
import * as authService from '../services/authService'
import { AuthContext } from './auth-context'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY))
  } catch {
    return null
  }
}

export default function AuthProvider({ children }) {
  const hasToken = Boolean(localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState(() => (hasToken ? readStoredUser() : null))
  const [checking, setChecking] = useState(hasToken)

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }, [])

  // Al abrir la app, valida el token guardado con el perfil del servidor
  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) return
    let active = true
    authService
      .getPerfil()
      .then((perfil) => {
        if (!active) return
        localStorage.setItem(USER_KEY, JSON.stringify(perfil))
        setUser(perfil)
      })
            .catch((error) => {
        // Solo se cierra la sesión si el servidor respondió con error (401/403).
        // Si no hay conexión, se conserva la sesión guardada.
        if (active && error.response) clearSession()
      })
      .finally(() => {
        if (active) setChecking(false)
      })
    return () => {
      active = false
    }
  }, [clearSession])

  // El interceptor de axios avisa cuando el servidor responde 401
  useEffect(() => {
    window.addEventListener('auth:logout', clearSession)
    return () => window.removeEventListener('auth:logout', clearSession)
  }, [clearSession])

  const login = useCallback(async (email, password) => {
    const data = await authService.login(email, password)
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.usuario))
    setUser(data.usuario)
    return data.usuario
  }, [])

  const logout = useCallback(async () => {
    try {
      await authService.logoutRequest()
    } catch {
      // Aunque falle la petición, la sesión local se cierra
    }
    clearSession()
  }, [clearSession])

  // Tras editar el perfil, se actualiza la sesión para que el menú muestre el nombre nuevo
  const updateUser = useCallback((usuario) => {
    localStorage.setItem(USER_KEY, JSON.stringify(usuario))
    setUser(usuario)
  }, [])

  const value = useMemo(
    () => ({ user, checking, login, logout, updateUser, isAuthenticated: Boolean(user) }),
    [user, checking, login, logout, updateUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}