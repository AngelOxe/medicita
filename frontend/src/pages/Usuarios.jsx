import { useState } from 'react'
import { Pencil, Plus, Search, Users } from 'lucide-react'
import UsuarioFormModal from '../components/usuarios/UsuarioFormModal'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import ConfirmModal from '../components/ui/ConfirmModal'
import DataTable from '../components/ui/DataTable'
import Select from '../components/ui/Select'
import StatusBadge from '../components/ui/StatusBadge'
import { useToast } from '../components/ui/useToast'
import useDebounce from '../hooks/useDebounce'
import usePaginatedList from '../hooks/usePaginatedList'
import { getErrorMessage } from '../lib/apiErrors'
import { roleLabel } from '../lib/menu'
import { desactivarUsuario, eliminarUsuario, listarUsuarios } from '../services/usuarioService'

const ROLES = [
  { value: '', label: 'Rol: todos' },
  { value: 'PACIENTE', label: 'Pacientes' },
  { value: 'MEDICO', label: 'Médico' },
  { value: 'ADMIN', label: 'Administradores' },
]

export default function Usuarios() {
  const { show } = useToast()
  const [busqueda, setBusqueda] = useState('')
  const [rol, setRol] = useState('')
  const search = useDebounce(busqueda, 400).trim()
  const list = usePaginatedList(listarUsuarios, { search, rol }, 10)

  const [formulario, setFormulario] = useState(null) // null | { usuario: objeto | null }
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
      await desactivarUsuario(aDesactivar.id)
      show('Usuario desactivado.', 'success')
      list.reload()
    } catch (error) {
      show(getErrorMessage(error), 'error')
    } finally {
      setProcesando(false)
      setADesactivar(null)
    }
  }

  const eliminar = async () => {
    setProcesando(true)
    try {
      await eliminarUsuario(aEliminar.id)
      show('Usuario eliminado.', 'success')
      list.reload()
    } catch (error) {
      // 422 si tiene citas o historial: en ese caso solo se puede desactivar
      show(getErrorMessage(error), 'error')
    } finally {
      setProcesando(false)
      setAEliminar(null)
    }
  }

  const columns = [
    {
      key: 'nombre',
      header: 'Nombre completo',
      render: (u) => (
        <div className="flex items-center gap-3">
          <Avatar nombre={u.nombre} avatarUrl={u.avatarUrl} />
          <span className="font-semibold text-ink">{u.nombre}</span>
        </div>
      ),
    },
    { key: 'email', header: 'Correo electrónico', render: (u) => u.email },
    { key: 'telefono', header: 'Teléfono', render: (u) => u.telefono },
    { key: 'rol', header: 'Rol', render: (u) => roleLabel[u.rol] ?? u.rol },
    { key: 'activo', header: 'Estado', render: (u) => <StatusBadge activo={u.activo} /> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (u) => (
        <div className="flex items-center gap-4 text-sm font-medium">
          <button type="button" onClick={() => setFormulario({ usuario: u })} className="inline-flex items-center gap-1 text-brand-600 hover:underline">
            <Pencil size={14} /> Editar
          </button>
          {u.activo && (
            <button type="button" onClick={() => setADesactivar(u)} className="text-danger hover:underline">
              Desactivar
            </button>
          )}
          <button type="button" onClick={() => setAEliminar(u)} className="text-danger hover:underline">
            Eliminar
          </button>
        </div>
      ),
    },
  ]

  const hayFiltros = Boolean(search || rol)

  return (
    <div>
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <h1 className="text-2xl font-bold text-ink">Usuarios</h1>
          <p className="mt-1 text-sm text-muted">Cuentas de pacientes, médico y administradores.</p>
        </div>
        <Button onClick={() => setFormulario({ usuario: null })}>
          <Plus size={16} /> Nuevo usuario
        </Button>
      </div>

      <div className="mb-4 flex flex-col gap-3 rounded-xl border border-line bg-white p-4 sm:flex-row">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
          <input
            type="search"
            aria-label="Buscar usuario"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre o correo..."
            className="w-full rounded-lg border border-line bg-white py-2.5 pl-9 pr-3 text-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-200"
          />
        </div>
        <Select aria-label="Filtrar por rol" value={rol} onChange={(e) => setRol(e.target.value)} options={ROLES} className="sm:w-48" />
      </div>

      <DataTable
        columns={columns}
        list={list}
        empty={
          <div className="flex flex-col items-center gap-2">
            <Users size={28} className="text-brand-600" />
            <p className="font-semibold text-ink">{hayFiltros ? 'No hay usuarios con esos filtros' : 'Aún no hay usuarios'}</p>
          </div>
        }
      />

      {formulario && (
        <UsuarioFormModal
          key={formulario.usuario?.id ?? 'nuevo'}
          usuario={formulario.usuario}
          onClose={() => setFormulario(null)}
          onSaved={guardado}
        />
      )}

      <ConfirmModal
        open={Boolean(aDesactivar)}
        title="¿Desactivar esta cuenta?"
        message={`${aDesactivar?.nombre} ya no podrá iniciar sesión. Sus citas se conservan.`}
        confirmText="Desactivar"
        variant="danger"
        loading={procesando}
        onConfirm={desactivar}
        onCancel={() => !procesando && setADesactivar(null)}
      />

      <ConfirmModal
        open={Boolean(aEliminar)}
        title="¿Eliminar esta cuenta?"
        message={`${aEliminar?.nombre} se borrará de forma definitiva. Si ya tiene citas o historial no se podrá eliminar; en ese caso desactívala.`}
        confirmText="Eliminar"
        variant="danger"
        loading={procesando}
        onConfirm={eliminar}
        onCancel={() => !procesando && setAEliminar(null)}
      />
    </div>
  )
}
