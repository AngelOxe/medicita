import { useEffect, useRef, useState } from 'react'
import { obtenerConfigGoogle } from '../../services/authService'

const GSI_SRC = 'https://accounts.google.com/gsi/client'
let scriptPromise = null

// Carga una sola vez la librería oficial "Google Identity Services"
function cargarScriptGoogle() {
  if (window.google?.accounts?.id) return Promise.resolve()
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = GSI_SRC
      script.async = true
      script.defer = true
      script.onload = () => resolve()
      script.onerror = () => {
        scriptPromise = null
        reject(new Error('No se pudo cargar Google'))
      }
      document.head.appendChild(script)
    })
  }
  return scriptPromise
}

/**
 * Botón "Continuar con Google". Solo aparece si el backend tiene configurado un ID de cliente
 * (GET /api/auth/google/config). Google entrega un token (credential) y se manda al backend,
 * que lo verifica y responde con el JWT de la aplicación.
 */
export default function GoogleButton({ onCredential, onError }) {
  const contenedor = useRef(null)
  const alCredencial = useRef(onCredential)
  const alError = useRef(onError)
  const [clientId, setClientId] = useState('')

  // Se mantienen las funciones más recientes sin reiniciar el botón de Google
  useEffect(() => {
    alCredencial.current = onCredential
    alError.current = onError
  })

  useEffect(() => {
    let activo = true
    obtenerConfigGoogle()
      .then((config) => {
        if (activo && config.habilitado) setClientId(config.clientId)
      })
      .catch(() => {
        // Sin configuración o sin conexión: el botón simplemente no se muestra
      })
    return () => {
      activo = false
    }
  }, [])

  useEffect(() => {
    if (!clientId) return
    let activo = true
    cargarScriptGoogle()
      .then(() => {
        if (!activo || !contenedor.current) return
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (respuesta) => alCredencial.current?.(respuesta.credential),
        })
        window.google.accounts.id.renderButton(contenedor.current, {
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'rectangular',
          logo_alignment: 'center',
          locale: 'es',
          width: Math.min(contenedor.current.offsetWidth || 320, 400),
        })
      })
      .catch(() => alError.current?.('No se pudo cargar el inicio de sesión con Google. Revisa tu conexión.'))
    return () => {
      activo = false
    }
  }, [clientId])

  if (!clientId) return null

  return (
    <div className="pt-1">
      <div className="my-4 flex items-center gap-3 text-xs text-muted">
        <span className="h-px flex-1 bg-line" />
        o continúa con
        <span className="h-px flex-1 bg-line" />
      </div>
      <div ref={contenedor} className="flex min-h-[44px] justify-center" />
    </div>
  )
}
