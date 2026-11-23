const ITEMS = [
  { clave: null, texto: 'Total de citas', color: 'text-ink' },
  { clave: 'confirmada', texto: 'Confirmadas', color: 'text-brand-600' },
  { clave: 'completada', texto: 'Completadas', color: 'text-ok' },
  { clave: 'cancelada', texto: 'Canceladas', color: 'text-danger' },
  { clave: 'no_asistio', texto: 'No asistió', color: 'text-warn' },
]

// Totales de citas por estado (porEstado viene de GET /api/dashboard/resumen)
export default function ResumenCitas({ porEstado }) {
  const total = Object.values(porEstado).reduce((a, b) => a + b, 0)
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {ITEMS.map((i) => (
        <div key={i.texto} className="rounded-xl border border-line bg-white px-4 py-3">
          <p className="text-xs text-muted">{i.texto}</p>
          <p className={`mt-1 text-2xl font-bold ${i.color}`}>{i.clave ? (porEstado[i.clave] ?? 0) : total}</p>
        </div>
      ))}
    </div>
  )
}
