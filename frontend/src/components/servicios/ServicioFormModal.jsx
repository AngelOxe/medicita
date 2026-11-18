import { useState } from 'react'
import Button from '../ui/Button'
import Input from '../ui/Input'
import Modal from '../ui/Modal'
import Textarea from '../ui/Textarea'
import useForm from '../../hooks/useForm'
import { getErrorMessage, getFieldErrors } from '../../lib/apiErrors'
import { actualizarServicio, crearServicio } from '../../services/servicioService'

// Mismas reglas que ServicioRequest.java (Bean Validation) y la base de datos
const rules = {
  nombre: (v) => {
    const s = (v ?? '').trim()
    if (!s) return 'El nombre es obligatorio.'
    if (s.length > 100) return 'Máximo 100 caracteres.'
    return ''
  },
  descripcion: (v) => ((v ?? '').length > 500 ? 'Máximo 500 caracteres.' : ''),
  duracionMin: (v) => {
    if (v === '' || v == null) return 'La duración es obligatoria.'
    if (!/^\d+$/.test(String(v)) || Number(v) < 1) return 'Escribe un número entero de minutos mayor que cero.'
    return ''
  },
  precio: (v) => {
    if (v === '' || v == null) return 'El precio es obligatorio.'
    if (!/^\d{1,8}(\.\d{1,2})?$/.test(String(v))) return 'Precio válido: hasta 8 enteros y 2 decimales, sin negativos.'
    return ''
  },
}

// Se monta solo cuando el modal está abierto (con `key`), así el formulario siempre arranca limpio
export default function ServicioFormModal({ servicio, onClose, onSaved }) {
  const editando = Boolean(servicio)
  const form = useForm(
    {
      nombre: servicio?.nombre ?? '',
      descripcion: servicio?.descripcion ?? '',
      duracionMin: servicio ? String(servicio.duracionMin) : '30',
      precio: servicio ? String(servicio.precio) : '',
      activo: servicio?.activo ?? true,
    },
    rules,
  )
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const guardar = async (e) => {
    e.preventDefault()
    if (!form.validateAll()) return

    const v = form.values
    const payload = {
      nombre: v.nombre.trim(),
      descripcion: v.descripcion.trim() || null,
      duracionMin: Number(v.duracionMin),
      precio: Number(v.precio),
      activo: v.activo,
    }

    setSaving(true)
    setFormError('')
    try {
      if (editando) await actualizarServicio(servicio.id, payload)
      else await crearServicio(payload)
      onSaved(editando ? 'Servicio actualizado.' : 'Servicio creado.')
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
      title={editando ? 'Editar servicio' : 'Nuevo servicio'}
      onClose={() => !saving && onClose()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancelar
          </Button>
          <Button type="submit" form="form-servicio" loading={saving}>
            {saving ? 'Guardando...' : 'Guardar'}
          </Button>
        </>
      }
    >
      <form id="form-servicio" onSubmit={guardar} noValidate className="space-y-4">
        {formError && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {formError}
          </p>
        )}
        <Input label="Nombre" placeholder="Consulta general" {...form.field('nombre')} />
        <Textarea
          label="Descripción"
          placeholder="Opcional"
          {...form.field('descripcion')}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Input label="Duración (minutos)" type="number" min="1" {...form.field('duracionMin')} />
          <Input label="Precio (MXN)" type="number" min="0" step="0.01" {...form.field('precio')} />
        </div>
        <label className="flex items-center gap-2 text-sm text-ink">
          <input
            type="checkbox"
            checked={form.values.activo}
            onChange={(e) => form.setValue('activo', e.target.checked)}
            className="h-4 w-4 accent-brand-600"
          />
          Servicio activo (disponible para reservar)
        </label>
      </form>
    </Modal>
  )
}
