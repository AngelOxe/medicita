import { useState } from 'react'
import Button from '../ui/Button'
import Input from '../ui/Input'
import Modal from '../ui/Modal'
import useForm from '../../hooks/useForm'
import { getErrorMessage, getFieldErrors } from '../../lib/apiErrors'
import { isoALocal, localAIso } from '../../lib/format'
import { actualizarBloqueo, crearBloqueo } from '../../services/bloqueoService'

const rules = {
  inicio: (v) => (v ? '' : 'El inicio es obligatorio.'),
  fin: (v, all) => {
    if (!v) return 'El fin es obligatorio.'
    if (all.inicio && v <= all.inicio) return 'El fin debe ser posterior al inicio.'
    return ''
  },
  motivo: (v) => {
    const s = (v ?? '').trim()
    if (!s) return 'El motivo es obligatorio.'
    if (s.length > 250) return 'Máximo 250 caracteres.'
    return ''
  },
}

export default function BloqueoFormModal({ bloqueo, onClose, onSaved }) {
  const editando = Boolean(bloqueo)
  const form = useForm(
    {
      inicio: isoALocal(bloqueo?.inicio),
      fin: isoALocal(bloqueo?.fin),
      motivo: bloqueo?.motivo ?? '',
    },
    rules,
  )
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const guardar = async (e) => {
    e.preventDefault()
    if (!form.validateAll()) return

    const v = form.values
    const payload = { inicio: localAIso(v.inicio), fin: localAIso(v.fin), motivo: v.motivo.trim() }

    setSaving(true)
    setFormError('')
    try {
      if (editando) await actualizarBloqueo(bloqueo.id, payload)
      else await crearBloqueo(payload)
      onSaved(editando ? 'Bloqueo actualizado.' : 'Bloqueo creado.')
    } catch (error) {
      const fields = getFieldErrors(error)
      if (Object.keys(fields).length) form.setServerErrors(fields)
      setFormError(getErrorMessage(error))
      setSaving(false)
    }
  }

  return (
    <Modal
      open
      title={editando ? 'Editar bloqueo' : 'Nuevo bloqueo'}
      onClose={() => !saving && onClose()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancelar
          </Button>
          <Button type="submit" form="form-bloqueo" loading={saving}>
            {saving ? 'Guardando...' : 'Guardar'}
          </Button>
        </>
      }
    >
      <form id="form-bloqueo" onSubmit={guardar} noValidate className="space-y-4">
        {formError && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {formError}
          </p>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          <Input label="Inicio" type="datetime-local" {...form.field('inicio')} />
          <Input label="Fin" type="datetime-local" {...form.field('fin')} />
        </div>
        <Input label="Motivo" placeholder="Ausencia del médico, congreso..." {...form.field('motivo')} />
        <p className="text-xs text-muted">Las horas son del consultorio (zona America/Mexico_City).</p>
      </form>
    </Modal>
  )
}
