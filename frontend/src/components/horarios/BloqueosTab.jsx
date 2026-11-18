import { useState } from 'react'
import { CalendarOff, Pencil, Plus } from 'lucide-react'
import Button from '../ui/Button'
import ConfirmModal from '../ui/ConfirmModal'
import DataTable from '../ui/DataTable'
import { useToast } from '../ui/useToast'
import BloqueoFormModal from './BloqueoFormModal'
import usePaginatedList from '../../hooks/usePaginatedList'
import { getErrorMessage } from '../../lib/apiErrors'
import { formatFechaHora } from '../../lib/format'
import { eliminarBloqueo, listarBloqueos } from '../../services/bloqueoService'

export default function BloqueosTab() {
  const { show } = useToast()
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const list = usePaginatedList(listarBloqueos, { desde, hasta }, 10)

  const [formulario, setFormulario] = useState(null)
  const [aEliminar, setAEliminar] = useState(null)
  const [procesando, setProcesando] = useState(false)

  const guardado = (mensaje) => {
    setFormulario(null)
    show(mensaje, 'success')
    list.reload()
  }

  const eliminar = async () => {
    setProcesando(true)
    try {
      await eliminarBloqueo(aEliminar.id)
      show('Bloqueo eliminado.', 'success')
      setAEliminar(null)
      list.reload()
    } catch (error) {
      show(getErrorMessage(error), 'error')
      setAEliminar(null)
    } finally {
      setProcesando(false)
    }
  }

  const columns = [
    { key: 'inicio', header: 'Inicio', render: (b) => formatFechaHora(b.inicio) },
    { key: 'fin', header: 'Fin', render: (b) => formatFechaHora(b.fin) },
    { key: 'motivo', header: 'Motivo', render: (b) => <span className="text-muted">{b.motivo}</span> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (b) => (
        <div className="flex items-center gap-4 text-sm font-medium">
          <button
            type="button"
            onClick={() => setFormulario({ bloqueo: b })}
            className="inline-flex items-center gap-1 text-brand-600 hover:underline"
          >
            <Pencil size={14} /> Editar
          </button>
          <button type="button" onClick={() => setAEliminar(b)} className="text-danger hover:underline">
            Eliminar
          </button>
        </div>
      ),
    },
  ]

  const inputFecha =
    'rounded-lg border border-line bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-200'

  return (
    <div>
      <div className="mb-4 flex flex-col gap-3 rounded-xl border border-line bg-white p-4 sm:flex-row sm:items-end">
        <label className="text-sm font-medium text-ink">
          <span className="mb-1.5 block">Desde</span>
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} className={inputFecha} />
        </label>
        <label className="text-sm font-medium text-ink">
          <span className="mb-1.5 block">Hasta</span>
          <input type="date" value={hasta} min={desde || undefined} onChange={(e) => setHasta(e.target.value)} className={inputFecha} />
        </label>
        {(desde || hasta) && (
          <button
            type="button"
            onClick={() => {
              setDesde('')
              setHasta('')
            }}
            className="pb-2.5 text-sm font-medium text-brand-600 hover:underline"
          >
            Limpiar filtros
          </button>
        )}
        <Button className="sm:ml-auto" onClick={() => setFormulario({ bloqueo: null })}>
          <Plus size={16} /> Nuevo bloqueo
        </Button>
      </div>

      <DataTable
        columns={columns}
        list={list}
        empty={
          <div className="flex flex-col items-center gap-2">
            <CalendarOff size={28} className="text-brand-600" />
            <p className="font-semibold text-ink">No hay bloqueos en ese periodo</p>
            <p>Los bloqueos quitan horarios de la agenda (ausencias, congresos, etc.).</p>
          </div>
        }
      />

      {formulario && (
        <BloqueoFormModal
          key={formulario.bloqueo?.id ?? 'nuevo'}
          bloqueo={formulario.bloqueo}
          onClose={() => setFormulario(null)}
          onSaved={guardado}
        />
      )}

      <ConfirmModal
        open={Boolean(aEliminar)}
        title="¿Eliminar este bloqueo?"
        message={aEliminar ? `"${aEliminar.motivo}". Esos horarios volverán a estar disponibles para reservar.` : ''}
        confirmText="Eliminar"
        variant="danger"
        loading={procesando}
        onConfirm={eliminar}
        onCancel={() => !procesando && setAEliminar(null)}
      />
    </div>
  )
}
