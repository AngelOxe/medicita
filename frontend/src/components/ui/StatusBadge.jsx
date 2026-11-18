export default function StatusBadge({ activo }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${
        activo ? 'bg-ok-bg text-ok' : 'bg-neutral-bg text-muted'
      }`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${activo ? 'bg-ok' : 'bg-muted'}`} />
      {activo ? 'Activo' : 'Inactivo'}
    </span>
  )
}
