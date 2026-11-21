import { ChevronLeft, ChevronRight } from 'lucide-react'

// Números a mostrar: 1 ... 4 5 6 ... 20 (page empieza en 0, igual que la API)
function pagesToShow(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const set = new Set([0, total - 1, current - 1, current, current + 1])
  const sorted = [...set].filter((p) => p >= 0 && p < total).sort((a, b) => a - b)
  const out = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) out.push('gap-' + p)
    out.push(p)
  })
  return out
}

export default function Pagination({ data, page, size, onPageChange, onSizeChange, sizes = [10, 20, 50] }) {
  const { totalElements, totalPages, content } = data
  const from = totalElements === 0 ? 0 : page * size + 1
  const to = page * size + content.length

  return (
    <div className="flex flex-col items-center justify-between gap-3 border-t border-line px-4 py-3 text-xs text-muted lg:flex-row">
      <span>
        Mostrando {from}-{to} de {totalElements} registros
      </span>

      <label className="flex items-center gap-2">
        Registros por página
        <select
          value={size}
          onChange={(e) => onSizeChange(Number(e.target.value))}
          className="rounded-md border border-line bg-white px-2 py-1 text-xs text-ink"
        >
          {sizes.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </label>

      <nav aria-label="Paginación" className="flex items-center gap-1">
        <button
          type="button"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          className="inline-flex items-center gap-1 rounded-md px-2 py-1 hover:bg-surface disabled:opacity-40"
        >
          <ChevronLeft size={14} /> Anterior
        </button>
        {pagesToShow(page, totalPages).map((p) =>
          typeof p === 'string' ? (
            <span key={p} className="px-1">
              …
            </span>
          ) : (
            <button
              key={p}
              type="button"
              aria-current={p === page ? 'page' : undefined}
              onClick={() => onPageChange(p)}
              className={`min-w-8 rounded-md px-2 py-1 font-medium ${
                p === page ? 'bg-brand-600 text-white' : 'hover:bg-surface'
              }`}
            >
              {p + 1}
            </button>
          ),
        )}
        <button
          type="button"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          className="inline-flex items-center gap-1 rounded-md px-2 py-1 hover:bg-surface disabled:opacity-40"
        >
          Siguiente <ChevronRight size={14} />
        </button>
      </nav>
    </div>
  )
}
