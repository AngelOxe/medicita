import { useEffect, useState } from 'react'
import Button from '../ui/Button'
import Input from '../ui/Input'
import Modal from '../ui/Modal'
import Select from '../ui/Select'
import Spinner from '../ui/Spinner'
import useForm from '../../hooks/useForm'
import { getErrorMessage, getFieldErrors } from '../../lib/apiErrors'
import { diasOptions, formatHora } from '../../lib/format'
import { actualizarHorario, crearHorario } from '../../services/horarioService'
import { listarServicios } from '../../services/servicioService'

const rules = {
  horaInicio: (v) => (v ? '' : 'La hora de inicio es obligatoria.'),
  horaFin: (v, all) => {
    if (!v) return 'La hora de fin es obligatoria.'
    if (all.horaInicio && v <= all.horaInicio) return 'La hora de fin debe ser posterior a la de inicio.'
    return ''
  },
  servicioIds: (v) => (v.length === 0 ? 'Selecciona al menos un servicio.' : ''),
}

// Relación N:M: aquí se eligen los servicios que se ofrecen en esta jornada
export default function HorarioFormModal({ horario, onClose, onSaved }) {
  const editando = Boolean(horario)
  const form = useForm(
    {
      diaSemana: String(horario?.diaSemana ?? 1),
      horaInicio: formatHora(horario?.horaInicio) || '09:00',
      horaFin: formatHora(horario?.horaFin) || '13:00',
      activo: horario?.activo ?? true,
      servicioIds: horario ? horario.servicios.map((s) => s.id) : [],
    },
    rules,
  )
  const [servicios, setServicios] = useState(null)
  const [errorCarga, setErrorCarga] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  useEffect(() => {
    listarServicios({ size: 100, activo: 'true' })
      .then((r) => setServicios(r.content))
      .catch((e) => setErrorCarga(getErrorMessage(e)))
  }, [])

  const alternar = (id) => {
    const actuales = form.values.servicioIds
    form.setValue('servicioIds', actuales.includes(id) ? actuales.filter((x) => x !== id) : [...actuales, id])
  }

  const guardar = async (e) => {
    e.preventDefault()
    if (!form.validateAll()) return

    const v = form.values
    const payload = {
      diaSemana: Number(v.diaSemana),
      horaInicio: `${v.horaInicio}:00`,
      horaFin: `${v.horaFin}:00`,
      activo: v.activo,
      servicioIds: v.servicioIds,
    }

    setSaving(true)
    setFormError('')
    try {
      if (editando) await actualizarHorario(horario.id, payload)
      else await crearHorario(payload)
      onSaved(editando ? 'Horario actualizado.' : 'Horario creado.')
    } catch (error) {
      const fields = getFieldErrors(error)
      if (Object.keys(fields).length) form.setServerErrors(fields)
      setFormError(getErrorMessage(error))
      setSaving(false)
    }
  }

  const errorServicios = form.field('servicioIds').error

  return (
    <Modal
      open
      title={editando ? 'Editar horario' : 'Nuevo horario'}
      onClose={() => !saving && onClose()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancelar
          </Button>
          <Button type="submit" form="form-horario" loading={saving}>
            {saving ? 'Guardando...' : 'Guardar'}
          </Button>
        </>
      }
    >
      <form id="form-horario" onSubmit={guardar} noValidate className="space-y-4">
        {formError && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {formError}
          </p>
        )}
        <Select label="Día de la semana" options={diasOptions} {...form.field('diaSemana')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Input label="Hora de inicio" type="time" {...form.field('horaInicio')} />
          <Input label="Hora de fin" type="time" {...form.field('horaFin')} />
        </div>

        <fieldset>
          <legend className="mb-1.5 text-sm font-medium text-ink">Servicios que se ofrecen en este horario</legend>
          <div className="max-h-48 overflow-y-auto rounded-lg border border-line p-3">
            {errorCarga && <p className="text-sm text-danger">{errorCarga}</p>}
            {!servicios && !errorCarga && (
              <div className="flex justify-center py-4 text-brand-600">
                <Spinner size="sm" />
              </div>
            )}
            {servicios?.length === 0 && <p className="text-sm text-muted">Primero crea al menos un servicio activo.</p>}
            <div className="grid gap-2 sm:grid-cols-2">
              {servicios?.map((s) => (
                <label key={s.id} className="flex items-center gap-2 text-sm text-ink">
                  <input
                    type="checkbox"
                    checked={form.values.servicioIds.includes(s.id)}
                    onChange={() => alternar(s.id)}
                    className="h-4 w-4 accent-brand-600"
                  />
                  {s.nombre}
                </label>
              ))}
            </div>
          </div>
          {errorServicios && (
            <p role="alert" className="mt-1 text-xs text-danger">
              {errorServicios}
            </p>
          )}
        </fieldset>

        <label className="flex items-center gap-2 text-sm text-ink">
          <input
            type="checkbox"
            checked={form.values.activo}
            onChange={(e) => form.setValue('activo', e.target.checked)}
            className="h-4 w-4 accent-brand-600"
          />
          Horario activo
        </label>
      </form>
    </Modal>
  )
}
