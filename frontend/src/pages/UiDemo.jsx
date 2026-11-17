import { useState } from 'react'
import Button from '../components/ui/Button'
import ConfirmModal from '../components/ui/ConfirmModal'
import Input from '../components/ui/Input'
import Spinner from '../components/ui/Spinner'
import { useToast } from '../components/ui/useToast'

export default function UiDemo() {
  const { show } = useToast()
  const [email, setEmail] = useState('')
  const [open, setOpen] = useState(false)
  const error = email && !email.includes('@') ? 'Escribe un correo válido' : ''

  return (
    <div className="mx-auto max-w-xl space-y-6 p-8">
      <h1 className="text-2xl font-bold text-sidebar">Componentes base</h1>
      <Input
        label="Correo electrónico"
        placeholder="doctor@consultorio.com"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        error={error}
      />
      <div className="flex flex-wrap gap-3">
        <Button onClick={() => show('Cita reservada', 'success')}>Mostrar aviso</Button>
        <Button variant="secondary">Secundario</Button>
        <Button variant="danger" onClick={() => setOpen(true)}>Cancelar cita</Button>
        <Button loading>Guardando</Button>
      </div>
      <Spinner className="text-brand-600" />
      <ConfirmModal
        open={open}
        title="¿Cancelar esta cita?"
        message="La cita quedará marcada como cancelada."
        confirmText="Cancelar cita"
        cancelText="Volver"
        variant="danger"
        onConfirm={() => {
          setOpen(false)
          show('Cita cancelada', 'info')
        }}
        onCancel={() => setOpen(false)}
      />
    </div>
  )
}