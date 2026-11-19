import { useEffect, useState } from 'react'
import { CalendarCheck, CalendarClock, CheckCircle2, Users } from 'lucide-react'
import Button from '../components/ui/Button'
import Spinner from '../components/ui/Spinner'
import { useAuth } from '../context/useAuth'
import { getErrorMessage } from '../lib/apiErrors'
import { obtenerResumen } from '../services/usuarioService'

const ESTADOS = [
  { clave: 'confirmada', texto: 'Confirmadas', barra: 'bg-brand-600' },
  { clave: 'completada', texto: 'Completadas', barra: 'bg-ok' },
  { clave: 'cancelada', texto: 'Canceladas', barra: 'bg-danger' },
  { clave: 'no_asistio', texto: 'No asistió', barra: 'bg-warn' },
]

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
  const [resumen, setResumen] = useState(null)
  const [error, setError] = useState('')
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let activo = true
    obtenerResumen()
      .then((d) => activo && (setResumen(d), setError('')))
      .catch((e) => activo && setError(getErrorMessage(e)))
    return () => {
      activo = false
    }
  }, [intento])

  const total = resumen ? Object.values(resumen.porEstado).reduce((a, b) => a + b, 0) : 0

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

      {!resumen && !error && (
        <div className="mt-10 flex justify-center text-brand-600">
          <Spinner />
        </div>
      )}

      {resumen && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Tarjeta icono={CalendarCheck} titulo="Citas de hoy" valor={resumen.citasHoy} />
            <Tarjeta icono={CalendarClock} titulo="Próximas confirmadas" valor={resumen.proximasConfirmadas} />
            <Tarjeta icono={Users} titulo="Pacientes activos" valor={resumen.pacientesActivos} />
            <Tarjeta icono={CheckCircle2} titulo="Completadas" valor={resumen.completadas} />
          </div>

          <section className="mt-6 rounded-xl border border-line bg-white p-5">
            <h2 className="text-sm font-semibold text-ink">Citas por estado</h2>
            <p className="text-xs text-muted">{total} citas en total</p>
            <ul className="mt-4 space-y-3">
              {ESTADOS.map((e) => {
                const n = resumen.porEstado[e.clave] ?? 0
                return (
                  <li key={e.clave}>
                    <div className="mb-1 flex justify-between text-sm">
                      <span className="text-ink">{e.texto}</span>
                      <span className="font-semibold text-ink">{n}</span>
                    </div>
                    <div className="h-2 rounded-full bg-neutral-bg">
                      <div className={`h-2 rounded-full ${e.barra}`} style={{ width: total ? `${(n / total) * 100}%` : '0%' }} />
                    </div>
                  </li>
                )
              })}
            </ul>
          </section>
        </>
      )}
    </div>
  )
}
