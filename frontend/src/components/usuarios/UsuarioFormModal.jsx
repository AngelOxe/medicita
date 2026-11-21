import { useState } from 'react'
import PasswordInput from '../auth/PasswordInput'
import Button from '../ui/Button'
import Input from '../ui/Input'
import Modal from '../ui/Modal'
import Select from '../ui/Select'
import useForm from '../../hooks/useForm'
import { getErrorMessage, getFieldErrors } from '../../lib/apiErrors'
import {
  PASSWORD_HELP,
  validateEmail,
  validateNombre,
  validatePassword,
  validateTelefono,
} from '../../lib/validators'
import { actualizarUsuario, crearUsuario } from '../../services/usuarioService'

const ROLES = [
  { value: 'PACIENTE', label: 'Paciente' },
  { value: 'MEDICO', label: 'Médico' },
  { value: 'ADMIN', label: 'Administrador' },
]

// Se monta solo con el modal abierto (con `key`): crea si no recibe usuario, edita si lo recibe
export default function UsuarioFormModal({ usuario, onClose, onSaved }) {
  const editando = Boolean(usuario)
  const form = useForm(
    {
      nombre: usuario?.nombre ?? '',
      email: usuario?.email ?? '',
      telefono: usuario?.telefono ?? '',
      password: '',
      rol: usuario?.rol ?? 'PACIENTE',
      activo: usuario?.activo ?? true,
    },
    {
      nombre: validateNombre,
      telefono: validateTelefono,
      // Correo, rol y contraseña solo se piden al crear
      email: (v) => (editando ? '' : validateEmail(v)),
      password: (v) => (editando ? '' : validatePassword(v)),
    },
  )
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const guardar = async (e) => {
    e.preventDefault()
    if (!form.validateAll()) return
    const v = form.values
    setSaving(true)
    setFormError('')
    try {
      if (editando) {
        await actualizarUsuario(usuario.id, { nombre: v.nombre.trim(), telefono: v.telefono.trim(), activo: v.activo })
      } else {
        await crearUsuario({
          nombre: v.nombre.trim(),
          email: v.email.trim(),
          telefono: v.telefono.trim(),
          password: v.password,
          rol: v.rol,
        })
      }
      onSaved(editando ? 'Usuario actualizado.' : 'Usuario creado.')
    } catch (error) {
      const fields = getFieldErrors(error)
      if (Object.keys(fields).length) form.setServerErrors(fields)
      else setFormError(getErrorMessage(error))
      setSaving(false)
    }
  }

  return (
    <Modal
      open
      title={editando ? 'Editar usuario' : 'Nuevo usuario'}
      onClose={() => !saving && onClose()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancelar
          </Button>
          <Button type="submit" form="form-usuario" loading={saving}>
            {saving ? 'Guardando...' : 'Guardar'}
          </Button>
        </>
      }
    >
      <form id="form-usuario" onSubmit={guardar} noValidate className="space-y-4">
        {formError && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {formError}
          </p>
        )}
        <Input label="Nombre completo" {...form.field('nombre')} />
        {editando ? (
          <p className="text-sm text-muted">
            Correo: <strong className="text-ink">{usuario.email}</strong> · Rol:{' '}
            <strong className="text-ink">{usuario.rol}</strong> (no se pueden cambiar aquí)
          </p>
        ) : (
          <Input label="Correo electrónico" type="email" autoComplete="off" {...form.field('email')} />
        )}
        <Input label="Teléfono" placeholder="+525512345678" {...form.field('telefono')} />
        {!editando && (
          <>
            <Select
              label="Rol"
              options={ROLES}
              value={form.values.rol}
              onChange={(e) => form.setValue('rol', e.target.value)}
              error={form.field('rol').error}
            />
            <PasswordInput label="Contraseña inicial" autoComplete="new-password" {...form.field('password')} />
            <p className="-mt-2 text-xs text-muted">{PASSWORD_HELP}</p>
          </>
        )}
        {editando && (
          <label className="flex items-center gap-2 text-sm text-ink">
            <input
              type="checkbox"
              checked={form.values.activo}
              onChange={(e) => form.setValue('activo', e.target.checked)}
              className="h-4 w-4 accent-brand-600"
            />
            Cuenta activa
          </label>
        )}
      </form>
    </Modal>
  )
}
