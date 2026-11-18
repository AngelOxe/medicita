// Quita los parametros vacios para no enviar ?search=&activo=
export function cleanParams(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined),
  )
}
