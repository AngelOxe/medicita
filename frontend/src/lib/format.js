// Zona horaria del consultorio (México no usa horario de verano desde 2022)
export const ZONA = 'America/Mexico_City'
const OFFSET_CONSULTORIO = '-06:00'

export const DIAS_SEMANA = {
  1: 'Lunes',
  2: 'Martes',
  3: 'Miércoles',
  4: 'Jueves',
  5: 'Viernes',
  6: 'Sábado',
  7: 'Domingo',
}

export const diasOptions = Object.entries(DIAS_SEMANA).map(([value, label]) => ({ value, label }))

const fechaHoraFmt = new Intl.DateTimeFormat('es-MX', {
  timeZone: ZONA,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

// "sv-SE" produce "2026-11-23 12:00", justo lo que necesita <input type="datetime-local">
const localFmt = new Intl.DateTimeFormat('sv-SE', {
  timeZone: ZONA,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

const precioFmt = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' })

export const formatFechaHora = (iso) => (iso ? fechaHoraFmt.format(new Date(iso)).replace(',', '') : '')
export const formatPrecio = (n) => precioFmt.format(Number(n ?? 0))

// "09:00:00" -> "09:00"
export const formatHora = (hora) => (hora ? hora.slice(0, 5) : '')

// "2026-11-23T12:00" (hora del consultorio) -> "2026-11-23T12:00:00-06:00"
export const localAIso = (local) => `${local}:00${OFFSET_CONSULTORIO}`

// Instante ISO -> valor para <input type="datetime-local"> en la hora del consultorio
export const isoALocal = (iso) => (iso ? localFmt.format(new Date(iso)).replace(' ', 'T') : '')
