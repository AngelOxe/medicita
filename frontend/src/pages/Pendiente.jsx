export default function Pendiente({ titulo }) {
  return (
    <div>
      <h1 className="text-2xl font-bold text-ink">{titulo}</h1>
      <p className="mt-1 text-sm text-muted">Esta sección se construirá en los siguientes días.</p>
    </div>
  )
}