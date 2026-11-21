import { ZONA } from './format'

// Las fechas del calendario viajan como texto "AAAA-MM-DD" (día en la zona del consultorio).
// La aritmética se hace en UTC solo para sumar días sin que el horario del navegador estorbe.
const pad = (n) => String(n).padStart(2, '0')
const aTexto = (d) => `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`
const aFecha = (s) => {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

export const sumarDias = (s, n) => {
  const d = aFecha(s)
  d.setUTCDate(d.getUTCDate() + n)
  return aTexto(d)
}

// 1 = lunes ... 7 = domingo (igual que diaSemana del backend)
export const diaSemanaISO = (s) => aFecha(s).getUTCDay() || 7
export const lunesDe = (s) => sumarDias(s, 1 - diaSemanaISO(s))

// Primer día del mes que está n meses después de s
export const sumarMeses = (s, n) => {
  const d = aFecha(s)
  d.setUTCDate(1)
  d.setUTCMonth(d.getUTCMonth() + n)
  return aTexto(d)
}
export const primerDiaMes = (s) => `${s.slice(0, 7)}-01`
export const mismoMes = (a, b) => a.slice(0, 7) === b.slice(0, 7)

const diaFmt = new Intl.DateTimeFormat('sv-SE', { timeZone: ZONA })
const horaFmt = new Intl.DateTimeFormat('en-GB', { timeZone: ZONA, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })

export const hoyISO = () => diaFmt.format(new Date())

// Instante ISO -> { fecha: 'AAAA-MM-DD', min: minutos desde las 00:00 } en la zona del consultorio
export function partesISO(iso) {
  const d = new Date(iso)
  const [h, m] = horaFmt.format(d).split(':').map(Number)
  return { fecha: diaFmt.format(d), min: h * 60 + m }
}

// "09:30:00" -> 570
export const minutosDe = (hora) => {
  const [h, m] = hora.split(':').map(Number)
  return h * 60 + m
}

export const horaDeMinutos = (min) => `${pad(Math.floor(min / 60))}:${pad(min % 60)}`

export const formatoFecha = (s, opciones) =>
  new Intl.DateTimeFormat('es-MX', { timeZone: 'UTC', ...opciones }).format(aFecha(s))
