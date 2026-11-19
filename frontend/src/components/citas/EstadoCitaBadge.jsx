const ESTILOS = {
  confirmada: { texto: 'Confirmada', clase: 'bg-brand-100 text-brand-700' },
  completada: { texto: 'Completada', clase: 'bg-ok-bg text-ok' },
  cancelada: { texto: 'Cancelada', clase: 'bg-danger-bg text-danger' },
  no_asistio: { texto: 'No asistió', clase: 'bg-warn-bg text-warn' },
}

export default function EstadoCitaBadge({ estado }) {
  const e = ESTILOS[estado] ?? { texto: estado, clase: 'bg-neutral-bg text-muted' }
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${e.clase}`}>
      {e.texto}
    </span>
  )
}
