const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const TEL_RE = /^\+[1-9]\d{7,14}$/

export const PASSWORD_HELP =
  'Mínimo 8 caracteres, con una mayúscula, un número y un símbolo.'

export function validateNombre(v) {
  const s = v.trim()
  if (!s) return 'El nombre es obligatorio.'
  if (s.length > 120) return 'Máximo 120 caracteres.'
  return ''
}

export function validateEmail(v) {
  const s = v.trim()
  if (!s) return 'El correo es obligatorio.'
  if (s.length > 254) return 'Máximo 254 caracteres.'
  if (!EMAIL_RE.test(s)) return 'Escribe un correo válido.'
  return ''
}

export function validateTelefono(v) {
  const s = v.trim()
  if (!s) return 'El teléfono es obligatorio.'
  if (!TEL_RE.test(s)) return 'Usa formato internacional, por ejemplo +525512345678.'
  return ''
}

export function validatePassword(v) {
  if (!v) return 'La contraseña es obligatoria.'
  if (v.length < 8) return 'Mínimo 8 caracteres.'
  if (!/[A-ZÁÉÍÓÚÑ]/.test(v)) return 'Debe incluir una mayúscula.'
  if (!/\d/.test(v)) return 'Debe incluir un número.'
  if (!/[^A-Za-z0-9\s]/.test(v)) return 'Debe incluir un símbolo.'
  if (new TextEncoder().encode(v).length > 72) return 'Máximo 72 bytes.'
  return ''
}

export function validateConfirm(password, confirm) {
  if (!confirm) return 'Confirma tu contraseña.'
  if (password !== confirm) return 'Las contraseñas no coinciden.'
  return ''
}