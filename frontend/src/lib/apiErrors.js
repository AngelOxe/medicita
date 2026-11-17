// Mensaje general para mostrar al usuario
export function getErrorMessage(error) {
  if (error.userMessage) return error.userMessage
  return error.response?.data?.message ?? 'Ocurrió un error inesperado. Inténtalo de nuevo.'
}

// Convierte fieldErrors del backend en { campo: mensaje } para ponerlos bajo cada input
export function getFieldErrors(error) {
  const lista = error.response?.data?.fieldErrors ?? []
  return Object.fromEntries(lista.map((e) => [e.field, e.message]))
}