import { useState } from 'react'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import { useToast } from '../components/ui/useToast'
import { useAuth } from '../context/useAuth'
import useForm from '../hooks/useForm'
import { getErrorMessage, getFieldErrors } from '../lib/apiErrors'
import { roleLabel } from '../lib/menu'
import { validateNombre, validateTelefono } from '../lib/validators'
import { actualizarPerfil } from '../services/usuarioService'

export default function Perfil() {
  const { user, updateUser } = useAuth()
  const { show } = useToast()
  const form = useForm(
    { nombre: user.nombre, telefono: user.telefono ?? '' },
    { nombre: validateNombre, telefono: validateTelefono },
  )
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const guardar = async (e) => {
    e.preventDefault()
    if (!form.validateAll()) return
    setSaving(true)
    setFormError('')
    try {
      const actualizado = await actualizarPerfil({
        nombre: form.values.nombre.trim(),
        telefono: form.values.telefono.trim(),
      })
      updateUser(actualizado)
      show('Perfil actualizado.', 'success')
    } catch (error) {
      const fields = getFieldErrors(error)
      if (Object.keys(fields).length) form.setServerErrors(fields)
      else setFormError(getErrorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-xl">
      <h1 className="text-2xl font-bold text-ink">Mi perfil</h1>
      <p className="mt-1 text-sm text-muted">Actualiza tu nombre y tu teléfono de contacto.</p>

      <div className="mt-6 rounded-xl border border-line bg-white p-6">
        <div className="mb-6 flex items-center gap-4">
          <Avatar nombre={user.nombre} avatarUrl={user.avatarUrl} className="!h-14 !w-14 !text-lg" />
          <div>
            <p className="font-semibold text-ink">{user.nombre}</p>
            <p className="text-sm text-muted">
              {user.email} · {roleLabel[user.rol] ?? user.rol}
            </p>
          </div>
        </div>

        <form onSubmit={guardar} noValidate className="space-y-4">
          {formError && (
            <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
              {formError}
            </p>
          )}
          <Input label="Nombre completo" {...form.field('nombre')} />
          <Input label="Teléfono" placeholder="+525512345678" {...form.field('telefono')} />
          <Button type="submit" loading={saving}>
            Guardar cambios
          </Button>
        </form>
      </div>
    </div>
  )
}
