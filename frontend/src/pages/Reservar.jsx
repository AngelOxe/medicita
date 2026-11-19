import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CalendarCheck, Clock3 } from 'lucide-react'
import Button from '../components/ui/Button'
import Spinner from '../components/ui/Spinner'
import { useToast } from '../components/ui/useToast'
import { getErrorMessage, getFieldErrors } from '../lib/apiErrors'
import { ZONA, formatPrecio } from '../lib/format'
import { listarServicios } from '../services/servicioService'
import { obtenerDisponibilidad, reservarCita } from '../services/citaService'

const horaFmt = new Intl.DateTimeFormat('es-MX', { timeZone: ZONA, hour: '2-digit', minute: '2-digit', hour12: false })
const diaFmt = new Intl.DateTimeFormat('es-MX', { timeZone: ZONA, weekday: 'long', day: 'numeric', month: 'long' })

// Fecha de hoy en la zona del consultorio, formato AAAA-MM-DD
const hoyISO = () => new Intl.DateTimeFormat('sv-SE', { timeZone: ZONA }).format(new Date())

export default function Reservar() {
  const navigate = useNavigate()
  const { show } = useToast()

  const [servicios, setServicios] = useState(null)
  const [errorServicios, setErrorServicios] = useState('')
  const [servicio, setServicio] = useState(null)
  const [fecha, setFecha] = useState(hoyISO())
  const [slots, setSlots] = useState(null) // null = cargando
  const [errorSlots, setErrorSlots] = useState('')
  const [slot, setSlot] = useState(null)
  const [reservando, setReservando] = useState(false)

  useEffect(() => {
    listarServicios({ activo: true, size: 50, page: 0 })
      .then((d) => setServicios(d.content))
      .catch((e) => setErrorServicios(getErrorMessage(e)))
  }, [])

  // Cada vez que cambia el servicio o la fecha se piden los horarios libres al servidor
  useEffect(() => {
    if (!servicio || !fecha) return undefined
    let activo = true
    obtenerDisponibilidad(servicio.id, fecha)
      .then((d) => {
        if (!activo) return
        setSlots(d.slots)
        setErrorSlots('')
      })
      .catch((e) => {
        if (!activo) return
        setSlots([])
        setErrorSlots(getFieldErrors(e).fecha || getErrorMessage(e))
      })
    return () => {
      activo = false
    }
  }, [servicio, fecha])

  const elegirServicio = (s) => {
    setServicio(s)
    setSlots(null)
    setSlot(null)
  }

  const cambiarFecha = (valor) => {
    setFecha(valor)
    setSlots(null)
    setSlot(null)
  }

  const confirmar = async () => {
    setReservando(true)
    try {
      await reservarCita({ servicioId: servicio.id, inicio: slot.inicio })
      show('Cita reservada. ¡Te esperamos!', 'success')
      navigate('/mis-citas')
    } catch (error) {
      show(getFieldErrors(error).inicio || getErrorMessage(error), 'error')
      // Si el horario ya lo tomó alguien más, se actualiza la lista de horarios libres
      setSlot(null)
      setSlots(null)
      obtenerDisponibilidad(servicio.id, fecha)
        .then((d) => setSlots(d.slots))
        .catch(() => setSlots([]))
    } finally {
      setReservando(false)
    }
  }

  const fechaLarga = useMemo(() => {
    if (!fecha) return ''
    return diaFmt.format(new Date(`${fecha}T12:00:00-06:00`))
  }, [fecha])

  return (
    <div className="max-w-4xl">
      <h1 className="text-2xl font-bold text-ink">Reservar cita</h1>
      <p className="mt-1 text-sm text-muted">Elige el servicio, el día y el horario que más te convenga.</p>

      <section className="mt-6">
        <h2 className="mb-3 text-sm font-semibold text-ink">1. Servicio</h2>
        {errorServicios && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">{errorServicios}</p>
        )}
        {!servicios && !errorServicios && <Spinner />}
        {servicios?.length === 0 && <p className="text-sm text-muted">No hay servicios disponibles por ahora.</p>}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {servicios?.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => elegirServicio(s)}
              aria-pressed={servicio?.id === s.id}
              className={`rounded-xl border p-4 text-left transition ${
                servicio?.id === s.id
                  ? 'border-brand-600 bg-brand-50 ring-2 ring-brand-200'
                  : 'border-line bg-white hover:border-brand-600'
              }`}
            >
              <p className="font-semibold text-ink">{s.nombre}</p>
              {s.descripcion && <p className="mt-1 line-clamp-2 text-xs text-muted">{s.descripcion}</p>}
              <p className="mt-3 flex items-center justify-between text-sm">
                <span className="inline-flex items-center gap-1 text-muted">
                  <Clock3 size={14} /> {s.duracionMin} min
                </span>
                <span className="font-semibold text-ink">{formatPrecio(s.precio)}</span>
              </p>
            </button>
          ))}
        </div>
      </section>

      {servicio && (
        <section className="mt-8">
          <h2 className="mb-3 text-sm font-semibold text-ink">2. Día y horario</h2>
          <div className="rounded-xl border border-line bg-white p-5">
            <label htmlFor="fecha" className="mb-1.5 block text-sm font-medium text-ink">
              Fecha
            </label>
            <input
              id="fecha"
              type="date"
              min={hoyISO()}
              value={fecha}
              onChange={(e) => cambiarFecha(e.target.value)}
              className="rounded-lg border border-line bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-200"
            />
            {fechaLarga && <p className="mt-2 text-sm text-muted first-letter:uppercase">{fechaLarga}</p>}

            <div className="mt-5">
              {slots === null && !errorSlots && <Spinner />}
              {errorSlots && (
                <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">{errorSlots}</p>
              )}
              {slots?.length === 0 && !errorSlots && (
                <p className="text-sm text-muted">
                  No hay horarios libres ese día para este servicio. Prueba con otra fecha.
                </p>
              )}
              {slots?.length > 0 && (
                <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-6">
                  {slots.map((s) => (
                    <button
                      key={s.inicio}
                      type="button"
                      onClick={() => setSlot(s)}
                      aria-pressed={slot?.inicio === s.inicio}
                      className={`rounded-lg border px-2 py-2 text-sm font-medium transition ${
                        slot?.inicio === s.inicio
                          ? 'border-brand-600 bg-brand-600 text-white'
                          : 'border-line bg-white text-ink hover:border-brand-600'
                      }`}
                    >
                      {horaFmt.format(new Date(s.inicio))}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </section>
      )}

      {servicio && slot && (
        <section className="mt-8 rounded-xl border border-brand-200 bg-brand-50 p-5">
          <h2 className="mb-2 flex items-center gap-2 text-sm font-semibold text-ink">
            <CalendarCheck size={16} className="text-brand-600" /> 3. Confirma tu cita
          </h2>
          <p className="text-sm text-ink">
            <strong>{servicio.nombre}</strong> el {fechaLarga} a las{' '}
            <strong>{horaFmt.format(new Date(slot.inicio))}</strong> ({servicio.duracionMin} min). Costo:{' '}
            <strong>{formatPrecio(servicio.precio)}</strong>.
          </p>
          <p className="mt-1 text-xs text-muted">Podrás cancelarla hasta 2 horas antes desde "Mis citas".</p>
          <Button className="mt-4" loading={reservando} onClick={confirmar}>
            Confirmar reserva
          </Button>
        </section>
      )}
    </div>
  )
}
