import { useEffect, useState } from 'react'
import { CalendarX2, Search } from 'lucide-react'
import Calendario from '../components/calendario/Calendario'
import EstadoCitaBadge from '../components/citas/EstadoCitaBadge'
import ResumenCitas from '../components/citas/ResumenCitas'
import ConfirmModal from '../components/ui/ConfirmModal'
import DataTable from '../components/ui/DataTable'
import Select from '../components/ui/Select'
import { useToast } from '../components/ui/useToast'
import useDebounce from '../hooks/useDebounce'
import usePaginatedList from '../hooks/usePaginatedList'
import { getErrorMessage } from '../lib/apiErrors'
import { ESTADOS_CITA } from '../lib/estadosCita'
import { formatFechaHora, formatPrecio } from '../lib/format'
import { cambiarEstadoCita, cancelarCita, listarCitas } from '../services/citaService'
import { obtenerResumen } from '../services/usuarioService'

const ACCIONES = {
  cancelar: {
    titulo: '¿Cancelar esta cita?',
    boton: 'Sí, cancelar',
    ok: 'Cita cancelada.',
    variant: 'danger',
    ejecutar: (c) => cancelarCita(c.id),
  },
  completada: {
    titulo: '¿Marcar como completada?',
    boton: 'Marcar completada',
    ok: 'Cita marcada como completada.',
    variant: 'primary',
    ejecutar: (c) => cambiarEstadoCita(c.id, 'completada'),
  },
  no_asistio: {
    titulo: '¿Marcar como "no asistió"?',
    boton: 'Marcar no asistió',
    ok: 'Cita marcada como "no asistió".',
    variant: 'danger',
    ejecutar: (c) => cambiarEstadoCita(c.id, 'no_asistio'),
  },
}

export default function GestionCitas() {
  const { show } = useToast()
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState('')
  const [fecha, setFecha] = useState('')
  const search = useDebounce(busqueda, 400).trim()
  const list = usePaginatedList(listarCitas, { search, estado, fecha }, 10)

  const [pendiente, setPendiente] = useState(null) // { accion, cita }
  const [procesando, setProcesando] = useState(false)
  const [vista, setVista] = useState('lista') // 'lista' | 'calendario'

  // Totales por estado de la parte de arriba; se vuelven a pedir cuando una cita cambia de estado
  const [resumen, setResumen] = useState(null)
  const [versionResumen, setVersionResumen] = useState(0)
  useEffect(() => {
    obtenerResumen()
      .then((d) => setResumen(d.porEstado))
      .catch(() => {})
  }, [versionResumen])

  const ejecutar = async () => {
    const { accion, cita } = pendiente
    setProcesando(true)
    try {
      await ACCIONES[accion].ejecutar(cita)
      show(ACCIONES[accion].ok, 'success')
      list.reload()
      setVersionResumen((n) => n + 1)
    } catch (error) {
      // Ej.: una cita que aún no termina no se puede marcar como completada (422)
      show(getErrorMessage(error), 'error')
    } finally {
      setProcesando(false)
      setPendiente(null)
    }
  }

  const columns = [
    {
      key: 'paciente',
      header: 'Paciente',
      render: (c) => <span className="font-semibold text-ink">{c.paciente.nombre}</span>,
    },
    { key: 'servicio', header: 'Servicio', render: (c) => c.servicio.nombre },
    { key: 'inicio', header: 'Fecha y hora', render: (c) => formatFechaHora(c.inicio) },
    { key: 'precio', header: 'Precio', render: (c) => formatPrecio(c.precioReservado) },
    { key: 'estado', header: 'Estado', render: (c) => <EstadoCitaBadge estado={c.estado} /> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (c) =>
        c.estado === 'confirmada' ? (
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm font-medium">
            <button type="button" onClick={() => setPendiente({ accion: 'completada', cita: c })} className="text-brand-600 hover:underline">
              Completada
            </button>
            <button type="button" onClick={() => setPendiente({ accion: 'no_asistio', cita: c })} className="text-warn hover:underline">
              No asistió
            </button>
            <button type="button" onClick={() => setPendiente({ accion: 'cancelar', cita: c })} className="text-danger hover:underline">
              Cancelar
            </button>
          </div>
        ) : (
          <span className="text-muted">—</span>
        ),
    },
  ]

  const hayFiltros = Boolean(search || estado || fecha)
  const accion = pendiente ? ACCIONES[pendiente.accion] : null

  return (
    <div>
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <h1 className="text-2xl font-bold text-ink">Gestión de citas</h1>
          <p className="mt-1 text-sm text-muted">Agenda completa del consultorio.</p>
        </div>
        <div role="group" aria-label="Forma de ver las citas" className="inline-flex self-start rounded-lg border border-line bg-white p-0.5">
          {[['lista', 'Lista'], ['calendario', 'Calendario']].map(([id, texto]) => (
            <button
              key={id}
              type="button"
              aria-pressed={vista === id}
              onClick={() => setVista(id)}
              className={`rounded-md px-3 py-1.5 text-sm font-medium ${vista === id ? 'bg-brand-600 text-white' : 'text-muted hover:text-ink'}`}
            >
              {texto}
            </button>
          ))}
        </div>
      </div>

      {resumen && (
        <div className="mb-6">
          <ResumenCitas porEstado={resumen} />
        </div>
      )}

      {vista === 'calendario' && <Calendario />}

      {vista === 'lista' && (
      <>
      <div className="mb-4 grid gap-3 rounded-xl border border-line bg-white p-4 sm:grid-cols-[1fr_12rem_11rem]">
        <div className="relative">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
          <input
            type="search"
            aria-label="Buscar por paciente"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre del paciente..."
            className="w-full rounded-lg border border-line bg-white py-2.5 pl-9 pr-3 text-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-200"
          />
        </div>
        <Select aria-label="Filtrar por estado" value={estado} onChange={(e) => setEstado(e.target.value)} options={ESTADOS_CITA} />
        <input
          type="date"
          aria-label="Filtrar por fecha"
          value={fecha}
          onChange={(e) => setFecha(e.target.value)}
          className="rounded-lg border border-line bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-200"
        />
      </div>

      <DataTable
        columns={columns}
        list={list}
        empty={
          <div className="flex flex-col items-center gap-2">
            <CalendarX2 size={28} className="text-brand-600" />
            <p className="font-semibold text-ink">{hayFiltros ? 'No hay citas con esos filtros' : 'Aún no hay citas'}</p>
          </div>
        }
      />
      </>
      )}

      <ConfirmModal
        open={Boolean(pendiente)}
        title={accion?.titulo ?? ''}
        message={
          pendiente
            ? `${pendiente.cita.paciente.nombre} · ${pendiente.cita.servicio.nombre} · ${formatFechaHora(pendiente.cita.inicio)}`
            : ''
        }
        confirmText={accion?.boton}
        variant={accion?.variant}
        loading={procesando}
        onConfirm={ejecutar}
        onCancel={() => !procesando && setPendiente(null)}
      />
    </div>
  )
}
