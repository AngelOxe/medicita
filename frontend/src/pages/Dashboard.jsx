import { useEffect, useState } from 'react'
import { CalendarCheck, CalendarClock, CheckCircle2, Users } from 'lucide-react'
import EstadoCitaBadge from '../components/citas/EstadoCitaBadge'
import ResumenCitas from '../components/citas/ResumenCitas'
import Button from '../components/ui/Button'
import Spinner from '../components/ui/Spinner'
import { useAuth } from '../context/useAuth'
import { getErrorMessage } from '../lib/apiErrors'
import { formatoFecha, hoyISO, partesISO, horaDeMinutos } from '../lib/fechas'
import { listarCitas } from '../services/citaService'
import { obtenerResumen } from '../services/usuarioService'

function Tarjeta({ icono: Icono, titulo, valor }) {
  return (
    <div className="rounded-xl border border-line bg-white p-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted">{titulo}</p>
        <Icono size={20} className="text-brand-600" />
      </div>
      <p className="mt-3 text-3xl font-bold text-ink">{valor}</p>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  const [datos, setDatos] = useState(null) // { resumen, hoy }
  const [error, setError] = useState('')
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let activo = true
    Promise.all([obtenerResumen(), listarCitas({ fecha: hoyISO(), size: 50, page: 0, orden: 'asc' })])
      .then(([resumen, hoy]) => activo && (setDatos({ resumen, hoy: hoy.content.filter((c) => c.estado !== 'cancelada') }), setError('')))
      .catch((e) => activo && setError(getErrorMessage(e)))
    return () => {
      activo = false
    }
  }, [intento])

  const resumen = datos?.resumen
  const hoy = hoyISO()

  return (
    <div>
      <h1 className="text-2xl font-bold text-ink">Hola, {user.nombre.split(' ')[0]}</h1>
      <p className="mt-1 text-sm text-muted">Resumen del consultorio.</p>

      {error && (
        <div role="alert" className="mt-6 rounded-xl border border-line bg-white p-8 text-center">
          <p className="text-sm text-danger">{error}</p>
          <Button variant="secondary" className="mt-4" onClick={() => { setError(''); setIntento((n) => n + 1) }}>
            Reintentar
          </Button>
        </div>
      )}

      {!datos && !error && (
        <div className="mt-10 flex justify-center text-brand-600">
          <Spinner />
        </div>
      )}

      {datos && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Tarjeta icono={CalendarCheck} titulo="Citas de hoy" valor={resumen.citasHoy} />
            <Tarjeta icono={CalendarClock} titulo="Próximas confirmadas" valor={resumen.proximasConfirmadas} />
            <Tarjeta icono={Users} titulo="Pacientes activos" valor={resumen.pacientesActivos} />
            <Tarjeta icono={CheckCircle2} titulo="Completadas" valor={resumen.completadas} />
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <section className="rounded-xl border border-line bg-white p-5">
              <h2 className="text-sm font-semibold text-ink">Agenda de hoy</h2>
              <p className="text-xs text-muted first-letter:uppercase">{formatoFecha(hoy, { weekday: 'long', day: 'numeric', month: 'long' })}</p>
              {datos.hoy.length === 0 ? (
                <p className="mt-6 text-center text-sm text-muted">No hay citas para hoy.</p>
              ) : (
                <ul className="mt-4 divide-y divide-line">
                  {datos.hoy.map((c) => (
                    <li key={c.id} className="flex items-center gap-3 py-3">
                      <span className="w-14 text-sm font-semibold text-brand-600">{horaDeMinutos(partesISO(c.inicio).min)}</span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-ink">{c.paciente.nombre}</p>
                        <p className="truncate text-xs text-muted">{c.servicio.nombre}</p>
                      </div>
                      <EstadoCitaBadge estado={c.estado} />
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="rounded-xl border border-line bg-white p-5">
              <h2 className="text-sm font-semibold text-ink">Actividad de la semana</h2>
              <p className="text-xs text-muted">Citas por día, sin contar las canceladas</p>
              <ul className="mt-4 divide-y divide-line">
                {resumen.semana.map((d) => (
                  <li key={d.fecha} className={`flex items-center justify-between py-2.5 text-sm ${d.fecha === hoy ? 'font-semibold text-brand-600' : 'text-ink'}`}>
                    <span className="inline-block first-letter:uppercase">{formatoFecha(d.fecha, { weekday: 'long', day: 'numeric', month: 'short' })}</span>
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${d.total ? 'bg-brand-50 text-brand-700' : 'bg-neutral-bg text-muted'}`}>
                      {d.total} {d.total === 1 ? 'cita' : 'citas'}
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          </div>

          <section className="mt-6">
            <h2 className="mb-3 text-sm font-semibold text-ink">Citas por estado</h2>
            <ResumenCitas porEstado={resumen.porEstado} />
          </section>
        </>
      )}
    </div>
  )
}
