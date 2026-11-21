import { useState } from 'react'
import { Clock3, Pencil, Plus } from 'lucide-react'
import Button from '../ui/Button'
import ConfirmModal from '../ui/ConfirmModal'
import DataTable from '../ui/DataTable'
import Select from '../ui/Select'
import StatusBadge from '../ui/StatusBadge'
import { useToast } from '../ui/useToast'
import HorarioFormModal from './HorarioFormModal'
import usePaginatedList from '../../hooks/usePaginatedList'
import { getErrorMessage } from '../../lib/apiErrors'
import { DIAS_SEMANA, diasOptions, formatHora } from '../../lib/format'
import {
  actualizarHorario,
  desactivarHorario,
  eliminarHorario,
  listarHorarios,
} from '../../services/horarioService'

const DIAS_FILTRO = [{ value: '', label: 'Todos los días' }, ...diasOptions]
const ESTADOS = [
  { value: '', label: 'Estado: todos' },
  { value: 'true', label: 'Activos' },
  { value: 'false', label: 'Inactivos' },
]

export default function HorariosTab() {
  const { show } = useToast()
  const [dia, setDia] = useState('')
  const [estado, setEstado] = useState('')
  const list = usePaginatedList(listarHorarios, { diaSemana: dia, activo: estado }, 10)

  const [formulario, setFormulario] = useState(null)
  const [aDesactivar, setADesactivar] = useState(null)
  const [aEliminar, setAEliminar] = useState(null)
  const [procesando, setProcesando] = useState(false)

  const guardado = (mensaje) => {
    setFormulario(null)
    show(mensaje, 'success')
    list.reload()
  }

  const desactivar = async () => {
    setProcesando(true)
    try {
      await desactivarHorario(aDesactivar.id)
      show('Horario desactivado.', 'success')
      setADesactivar(null)
      list.reload()
    } catch (error) {
      // Por ejemplo: 422 si hay citas futuras que quedarían sin horario
      show(getErrorMessage(error), 'error')
      setADesactivar(null)
    } finally {
      setProcesando(false)
    }
  }

  const eliminar = async () => {
    setProcesando(true)
    try {
      await eliminarHorario(aEliminar.id)
      show('Horario eliminado.', 'success')
      list.reload()
    } catch (error) {
      // 422 si dejaría citas futuras sin horario que las cubra
      show(getErrorMessage(error), 'error')
    } finally {
      setProcesando(false)
      setAEliminar(null)
    }
  }

  const activar = async (h) => {
    try {
      await actualizarHorario(h.id, {
        diaSemana: h.diaSemana,
        horaInicio: h.horaInicio,
        horaFin: h.horaFin,
        activo: true,
        servicioIds: h.servicios.map((s) => s.id),
      })
      show('Horario activado.', 'success')
      list.reload()
    } catch (error) {
      show(getErrorMessage(error), 'error')
    }
  }

  const columns = [
    { key: 'dia', header: 'Día', render: (h) => <span className="font-semibold">{DIAS_SEMANA[h.diaSemana]}</span> },
    { key: 'horas', header: 'Horario', render: (h) => `${formatHora(h.horaInicio)} – ${formatHora(h.horaFin)}` },
    {
      key: 'servicios',
      header: 'Servicios que se ofrecen',
      render: (h) => {
        // Se muestran 3 y el resto como "+N más" (el nombre completo queda en el título)
        const visibles = h.servicios.slice(0, 3)
        const resto = h.servicios.length - visibles.length
        return (
          <div className="flex max-w-md flex-wrap gap-1.5">
            {visibles.map((s) => (
              <span key={s.id} className="rounded-full bg-brand-50 px-2.5 py-0.5 text-xs font-medium text-brand-700">
                {s.nombre}
              </span>
            ))}
            {resto > 0 && (
              <span
                title={h.servicios.slice(3).map((s) => s.nombre).join(', ')}
                className="rounded-full bg-neutral-bg px-2.5 py-0.5 text-xs font-medium text-muted"
              >
                +{resto} más
              </span>
            )}
          </div>
        )
      },
    },
    { key: 'activo', header: 'Estado', render: (h) => <StatusBadge activo={h.activo} /> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (h) => (
        <div className="flex items-center gap-4 text-sm font-medium">
          <button
            type="button"
            onClick={() => setFormulario({ horario: h })}
            className="inline-flex items-center gap-1 text-brand-600 hover:underline"
          >
            <Pencil size={14} /> Editar
          </button>
          {h.activo ? (
            <button type="button" onClick={() => setADesactivar(h)} className="text-danger hover:underline">
              Desactivar
            </button>
          ) : (
            <button type="button" onClick={() => activar(h)} className="text-brand-600 hover:underline">
              Activar
            </button>
          )}
          <button type="button" onClick={() => setAEliminar(h)} className="text-danger hover:underline">
            Eliminar
          </button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <div className="mb-4 flex flex-col gap-3 rounded-xl border border-line bg-white p-4 sm:flex-row sm:items-center">
        <Select aria-label="Filtrar por día" value={dia} onChange={(e) => setDia(e.target.value)} options={DIAS_FILTRO} className="sm:w-48" />
        <Select aria-label="Filtrar por estado" value={estado} onChange={(e) => setEstado(e.target.value)} options={ESTADOS} className="sm:w-48" />
        <Button className="sm:ml-auto" onClick={() => setFormulario({ horario: null })}>
          <Plus size={16} /> Nuevo horario
        </Button>
      </div>

      <DataTable
        columns={columns}
        list={list}
        empty={
          <div className="flex flex-col items-center gap-2">
            <Clock3 size={28} className="text-brand-600" />
            <p className="font-semibold text-ink">No hay horarios con esos filtros</p>
            <p>Crea una jornada de atención con "Nuevo horario".</p>
          </div>
        }
      />

      {formulario && (
        <HorarioFormModal
          key={formulario.horario?.id ?? 'nuevo'}
          horario={formulario.horario}
          onClose={() => setFormulario(null)}
          onSaved={guardado}
        />
      )}

      <ConfirmModal
        open={Boolean(aDesactivar)}
        title="¿Desactivar este horario?"
        message={
          aDesactivar
            ? `${DIAS_SEMANA[aDesactivar.diaSemana]} de ${formatHora(aDesactivar.horaInicio)} a ${formatHora(aDesactivar.horaFin)}. Ya no se ofrecerán citas en esa jornada.`
            : ''
        }
        confirmText="Desactivar"
        variant="danger"
        loading={procesando}
        onConfirm={desactivar}
        onCancel={() => !procesando && setADesactivar(null)}
      />

      <ConfirmModal
        open={Boolean(aEliminar)}
        title="¿Eliminar este horario?"
        message={
          aEliminar
            ? `${DIAS_SEMANA[aEliminar.diaSemana]} de ${formatHora(aEliminar.horaInicio)} a ${formatHora(aEliminar.horaFin)} se borrará de forma definitiva. Si hay citas futuras que dependan de él, no se podrá eliminar.`
            : ''
        }
        confirmText="Eliminar"
        variant="danger"
        loading={procesando}
        onConfirm={eliminar}
        onCancel={() => !procesando && setAEliminar(null)}
      />
    </div>
  )
}
