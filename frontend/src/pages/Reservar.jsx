import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CalendarCheck, Clock3, Info, Stethoscope } from 'lucide-react'
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

const PASO_MS = 30 * 60 * 1000 // los horarios se ofrecen cada 30 minutos
const DIAS_BUSQUEDA = 14

// Suma días a una fecha AAAA-MM-DD sin depender de la zona horaria
const sumarDias = (iso, n) => {
  const d = new Date(`${iso}T12:00:00Z`)
  d.setUTCDate(d.getUTCDate() + n)
  return d.toISOString().slice(0, 10)
}

const AVISOS = [
  'Llega 10 minutos antes de tu hora para que la consulta empiece puntual.',
  'Trae una identificación y, si los tienes, estudios o recetas anteriores.',
  'Puedes cancelar sin costo hasta 2 horas antes desde "Mis citas".',
  'Si no puedes asistir, cancela para que otro paciente use ese horario.',
]

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
  const horarioRef = useRef(null)
  const confirmarRef = useRef(null)
  const busqueda = useRef({ activa: false, dias: 0 }) // búsqueda automática del próximo día con horarios

  // Al elegir servicio o un horario, la pantalla baja sola a la parte que sigue
  useEffect(() => {
    if (servicio) horarioRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [servicio])

  useEffect(() => {
    if (slot) confirmarRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [slot])

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
        // Si el día no tiene horarios libres, se prueba el siguiente (hasta DIAS_BUSQUEDA días)
        const auto = busqueda.current
        if (d.slots.length === 0 && auto.activa && auto.dias < DIAS_BUSQUEDA) {
          auto.dias += 1
          setFecha(sumarDias(fecha, 1))
          return
        }
        auto.activa = false
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
    if (servicio?.id === s.id) return // mismo servicio: nada que recargar
    busqueda.current = { activa: true, dias: 0 }
    setServicio(s)
    setFecha(hoyISO())
    setSlots(null)
    setSlot(null)
  }

  const cambiarFecha = (valor) => {
    busqueda.current.activa = false
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

  // Todas las horas entre el primer y el último horario libre; las que no están libres se marcan como ocupadas
  const marcas = useMemo(() => {
    if (!slots?.length) return []
    const libres = new Map(slots.map((s) => [new Date(s.inicio).getTime(), s]))
    const desde = Math.min(...libres.keys())
    const hasta = Math.max(...libres.keys())
    const lista = []
    for (let t = desde; t <= hasta; t += PASO_MS) lista.push({ t, slot: libres.get(t) ?? null })
    return lista
  }, [slots])

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
              <span className="mb-3 flex h-9 w-9 items-center justify-center rounded-lg bg-brand-100 text-brand-600">
                <Stethoscope size={18} />
              </span>
              <p className="font-semibold text-ink">{s.nombre}</p>
              {s.descripcion && <p className="mt-1 text-xs leading-relaxed text-muted">{s.descripcion}</p>}
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
        <section ref={horarioRef} className="mt-8 scroll-mt-4">
          <h2 className="mb-3 text-sm font-semibold text-ink">2. Día y horario: {servicio.nombre}</h2>
          <div className="rounded-xl border border-line bg-white p-5">
            <p className="mb-4 text-sm text-muted">
              Duración aproximada de <strong className="text-ink">{servicio.duracionMin} minutos</strong> y costo de{' '}
              <strong className="text-ink">{formatPrecio(servicio.precio)}</strong>, que se paga en el consultorio.
            </p>
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
              {marcas.length > 0 && (
                <>
                  <ul className="mb-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted">
                    <li className="inline-flex items-center gap-1.5">
                      <span className="h-3 w-3 rounded border border-green-300 bg-green-100" /> Disponible
                    </li>
                    <li className="inline-flex items-center gap-1.5">
                      <span className="h-3 w-3 rounded bg-neutral-bg" /> No disponible
                    </li>
                    <li className="inline-flex items-center gap-1.5">
                      <span className="h-3 w-3 rounded bg-brand-600" /> Tu elección
                    </li>
                  </ul>
                  <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-6">
                    {marcas.map(({ t, slot: libre }) =>
                      libre ? (
                        <button
                          key={t}
                          type="button"
                          onClick={() => setSlot(libre)}
                          aria-pressed={slot?.inicio === libre.inicio}
                          className={`rounded-lg border px-2 py-2 text-sm font-medium transition ${
                            slot?.inicio === libre.inicio
                              ? 'border-brand-600 bg-brand-600 text-white'
                              : 'border-green-300 bg-green-100 text-ink hover:border-brand-600'
                          }`}
                        >
                          {horaFmt.format(new Date(t))}
                        </button>
                      ) : (
                        <span
                          key={t}
                          aria-label={`${horaFmt.format(new Date(t))}, no disponible`}
                          className="rounded-lg bg-neutral-bg px-2 py-2 text-center text-sm text-muted line-through"
                        >
                          {horaFmt.format(new Date(t))}
                        </span>
                      ),
                    )}
                  </div>
                </>
              )}
            </div>
          </div>
        </section>
      )}

      {servicio && slot && (
        <section ref={confirmarRef} className="mt-8 rounded-xl border border-brand-200 bg-brand-50 p-5">
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

      {servicio && (
        <section className="mt-8 rounded-xl border border-line bg-white p-5">
          <h2 className="flex items-center gap-2 text-sm font-semibold text-ink">
            <Info size={16} className="text-brand-600" /> Antes de tu cita
          </h2>
          <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-muted">
            {AVISOS.map((a) => (
              <li key={a}>{a}</li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
