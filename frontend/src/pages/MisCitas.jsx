import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarX2 } from 'lucide-react'
import EstadoCitaBadge from '../components/citas/EstadoCitaBadge'
import Button from '../components/ui/Button'
import ConfirmModal from '../components/ui/ConfirmModal'
import DataTable from '../components/ui/DataTable'
import { useToast } from '../components/ui/useToast'
import usePaginatedList from '../hooks/usePaginatedList'
import { getErrorMessage } from '../lib/apiErrors'
import { formatFechaHora, formatPrecio } from '../lib/format'
import { cancelarCita, listarMisCitas } from '../services/citaService'

const TABS = [
  { value: 'proximas', label: 'Próximas' },
  { value: 'historial', label: 'Historial' },
]

export default function MisCitas() {
  const { show } = useToast()
  const [tipo, setTipo] = useState('proximas')
  const list = usePaginatedList(listarMisCitas, { tipo }, 10)

  const [aCancelar, setACancelar] = useState(null)
  const [procesando, setProcesando] = useState(false)

  const cancelar = async () => {
    setProcesando(true)
    try {
      await cancelarCita(aCancelar.id)
      show('Cita cancelada.', 'success')
      setACancelar(null)
      list.reload()
    } catch (error) {
      // Ej.: "Solo puedes cancelar con 2 horas o mas de anticipacion" (422)
      show(getErrorMessage(error), 'error')
      setACancelar(null)
    } finally {
      setProcesando(false)
    }
  }

  const columns = [
    {
      key: 'servicio',
      header: 'Servicio',
      render: (c) => <span className="font-semibold text-ink">{c.servicio.nombre}</span>,
    },
    { key: 'inicio', header: 'Fecha y hora', render: (c) => formatFechaHora(c.inicio) },
    { key: 'precio', header: 'Precio', render: (c) => formatPrecio(c.precioReservado) },
    { key: 'estado', header: 'Estado', render: (c) => <EstadoCitaBadge estado={c.estado} /> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (c) =>
        c.estado === 'confirmada' && new Date(c.inicio) > new Date() ? (
          <button type="button" onClick={() => setACancelar(c)} className="text-sm font-medium text-danger hover:underline">
            Cancelar
          </button>
        ) : (
          <span className="text-muted">—</span>
        ),
    },
  ]

  return (
    <div>
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <h1 className="text-2xl font-bold text-ink">Mis citas</h1>
          <p className="mt-1 text-sm text-muted">Consulta, y si lo necesitas cancela, tus citas.</p>
        </div>
        <Link to="/reservar">
          <Button>Reservar cita</Button>
        </Link>
      </div>

      <div role="tablist" className="mb-4 inline-flex rounded-lg border border-line bg-white p-1">
        {TABS.map((t) => (
          <button
            key={t.value}
            role="tab"
            type="button"
            aria-selected={tipo === t.value}
            onClick={() => setTipo(t.value)}
            className={`rounded-md px-4 py-1.5 text-sm font-medium transition ${
              tipo === t.value ? 'bg-brand-600 text-white' : 'text-muted hover:text-ink'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <DataTable
        columns={columns}
        list={list}
        empty={
          <div className="flex flex-col items-center gap-2">
            <CalendarX2 size={28} className="text-brand-600" />
            <p className="font-semibold text-ink">
              {tipo === 'proximas' ? 'No tienes citas próximas' : 'Aún no tienes historial'}
            </p>
            {tipo === 'proximas' && <p>Reserva la primera con "Reservar cita".</p>}
          </div>
        }
      />

      <ConfirmModal
        open={Boolean(aCancelar)}
        title="¿Cancelar esta cita?"
        message={
          aCancelar
            ? `${aCancelar.servicio.nombre} el ${formatFechaHora(aCancelar.inicio)}. Solo puedes cancelar con 2 horas o más de anticipación.`
            : ''
        }
        confirmText="Sí, cancelar"
        cancelText="Conservarla"
        variant="danger"
        loading={procesando}
        onConfirm={cancelar}
        onCancel={() => !procesando && setACancelar(null)}
      />
    </div>
  )
}
