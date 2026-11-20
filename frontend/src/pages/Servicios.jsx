import { useState } from 'react'
import { ClipboardList, Pencil, Plus, Search } from 'lucide-react'
import Button from '../components/ui/Button'
import ConfirmModal from '../components/ui/ConfirmModal'
import DataTable from '../components/ui/DataTable'
import Select from '../components/ui/Select'
import StatusBadge from '../components/ui/StatusBadge'
import { useToast } from '../components/ui/useToast'
import ServicioFormModal from '../components/servicios/ServicioFormModal'
import useDebounce from '../hooks/useDebounce'
import usePaginatedList from '../hooks/usePaginatedList'
import { getErrorMessage } from '../lib/apiErrors'
import { formatPrecio } from '../lib/format'
import {
  actualizarServicio,
  desactivarServicio,
  eliminarServicio,
  listarServicios,
} from '../services/servicioService'

const ESTADOS = [
  { value: '', label: 'Estado: todos' },
  { value: 'true', label: 'Activos' },
  { value: 'false', label: 'Inactivos' },
]

export default function Servicios() {
  const { show } = useToast()

  // Los filtros viajan a la API como parámetros: ?search=...&activo=...
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState('')
  const search = useDebounce(busqueda, 400).trim()
  const list = usePaginatedList(listarServicios, { search, activo: estado }, 10)

  const [formulario, setFormulario] = useState(null) // null | { servicio: objeto | null }
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
      await desactivarServicio(aDesactivar.id)
      show('Servicio desactivado.', 'success')
      setADesactivar(null)
      list.reload()
    } catch (error) {
      show(getErrorMessage(error), 'error')
    } finally {
      setProcesando(false)
    }
  }

  const eliminar = async () => {
    setProcesando(true)
    try {
      await eliminarServicio(aEliminar.id)
      show('Servicio eliminado.', 'success')
      list.reload()
    } catch (error) {
      // 422 si ya tuvo citas: en ese caso solo se puede desactivar
      show(getErrorMessage(error), 'error')
    } finally {
      setProcesando(false)
      setAEliminar(null)
    }
  }

  const activar = async (s) => {
    try {
      await actualizarServicio(s.id, {
        nombre: s.nombre,
        descripcion: s.descripcion,
        duracionMin: s.duracionMin,
        precio: s.precio,
        activo: true,
      })
      show('Servicio activado.', 'success')
      list.reload()
    } catch (error) {
      show(getErrorMessage(error), 'error')
    }
  }

  const columns = [
    {
      key: 'nombre',
      header: 'Servicio',
      render: (s) => (
        <div>
          <p className="font-semibold text-ink">{s.nombre}</p>
          {s.descripcion && <p className="max-w-sm truncate text-xs text-muted">{s.descripcion}</p>}
        </div>
      ),
    },
    { key: 'duracionMin', header: 'Duración', render: (s) => `${s.duracionMin} min` },
    {
      key: 'precio',
      header: 'Precio',
      render: (s) => <span className="font-semibold">{formatPrecio(s.precio)}</span>,
    },
    { key: 'activo', header: 'Estado', render: (s) => <StatusBadge activo={s.activo} /> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (s) => (
        <div className="flex items-center gap-4 text-sm font-medium">
          <button
            type="button"
            onClick={() => setFormulario({ servicio: s })}
            className="inline-flex items-center gap-1 text-brand-600 hover:underline"
          >
            <Pencil size={14} /> Editar
          </button>
          {s.activo ? (
            <button type="button" onClick={() => setADesactivar(s)} className="text-danger hover:underline">
              Desactivar
            </button>
          ) : (
            <button type="button" onClick={() => activar(s)} className="text-brand-600 hover:underline">
              Activar
            </button>
          )}
          <button type="button" onClick={() => setAEliminar(s)} className="text-danger hover:underline">
            Eliminar
          </button>
        </div>
      ),
    },
  ]

  const hayFiltros = Boolean(search || estado)

  return (
    <div>
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <h1 className="text-2xl font-bold text-ink">Servicios</h1>
          <p className="mt-1 text-sm text-muted">Catálogo de servicios del consultorio.</p>
        </div>
        <Button onClick={() => setFormulario({ servicio: null })}>
          <Plus size={16} /> Nuevo servicio
        </Button>
      </div>

      <div className="mb-4 flex flex-col gap-3 rounded-xl border border-line bg-white p-4 sm:flex-row">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
          <input
            type="search"
            aria-label="Buscar servicio"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre o descripción..."
            className="w-full rounded-lg border border-line bg-white py-2.5 pl-9 pr-3 text-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-200"
          />
        </div>
        <Select
          aria-label="Filtrar por estado"
          value={estado}
          onChange={(e) => setEstado(e.target.value)}
          options={ESTADOS}
          className="sm:w-48"
        />
      </div>

      <DataTable
        columns={columns}
        list={list}
        empty={
          <div className="flex flex-col items-center gap-2">
            <ClipboardList size={28} className="text-brand-600" />
            <p className="font-semibold text-ink">
              {hayFiltros ? 'No hay servicios con esos filtros' : 'Aún no hay servicios'}
            </p>
            <p>{hayFiltros ? 'Prueba con otra búsqueda.' : 'Crea el primero con "Nuevo servicio".'}</p>
          </div>
        }
      />

      {formulario && (
        <ServicioFormModal
          key={formulario.servicio?.id ?? 'nuevo'}
          servicio={formulario.servicio}
          onClose={() => setFormulario(null)}
          onSaved={guardado}
        />
      )}

      <ConfirmModal
        open={Boolean(aDesactivar)}
        title="¿Desactivar este servicio?"
        message={`"${aDesactivar?.nombre}" ya no se podrá reservar. Las citas anteriores conservan su información.`}
        confirmText="Desactivar"
        variant="danger"
        loading={procesando}
        onConfirm={desactivar}
        onCancel={() => !procesando && setADesactivar(null)}
      />

      <ConfirmModal
        open={Boolean(aEliminar)}
        title="¿Eliminar este servicio?"
        message={`"${aEliminar?.nombre}" se borrará de forma definitiva. Si ya tiene citas registradas no se podrá eliminar; en ese caso desactívalo.`}
        confirmText="Eliminar"
        variant="danger"
        loading={procesando}
        onConfirm={eliminar}
        onCancel={() => !procesando && setAEliminar(null)}
      />
    </div>
  )
}
