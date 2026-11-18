import Button from './Button'
import Pagination from './Pagination'
import Spinner from './Spinner'
import { getErrorMessage } from '../../lib/apiErrors'

/**
 * Tabla con paginación del servidor, estado de carga, error de red con "Reintentar" y estado vacío.
 * columns = [{ key, header, render?(fila) }] · list = lo que devuelve usePaginatedList.
 * En móvil la tabla tiene scroll horizontal.
 */
export default function DataTable({ columns, list, empty, sizes, rowKey = (row) => row.id }) {
  const { data, error, loading, page, setPage, size, setSize, reload } = list

  if (!data && error) {
    return (
      <div role="alert" className="rounded-xl border border-line bg-white p-10 text-center">
        <p className="text-sm text-danger">{getErrorMessage(error)}</p>
        <Button variant="secondary" className="mt-4" onClick={reload}>
          Reintentar
        </Button>
      </div>
    )
  }

  if (!data) {
    return (
      <div className="flex justify-center rounded-xl border border-line bg-white p-12 text-brand-600">
        <Spinner />
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-xl border border-line bg-white" aria-busy={loading}>
      {error && (
        <p role="alert" className="bg-danger-bg px-4 py-2 text-center text-xs text-danger">
          {getErrorMessage(error)}{' '}
          <button type="button" onClick={reload} className="font-semibold underline">
            Reintentar
          </button>
        </p>
      )}

      {data.content.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-muted">{empty}</div>
      ) : (
        <div className={`overflow-x-auto transition-opacity ${loading ? 'opacity-50' : ''}`}>
          <table className="w-full min-w-[640px] border-collapse text-sm">
            <thead>
              <tr className="bg-surface text-left text-xs font-semibold text-muted">
                {columns.map((c) => (
                  <th key={c.key} className="px-4 py-3">
                    {c.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.content.map((row) => (
                <tr key={rowKey(row)} className="border-t border-line hover:bg-surface/60">
                  {columns.map((c) => (
                    <td key={c.key} className="px-4 py-3 align-middle">
                      {c.render ? c.render(row) : row[c.key]}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data.content.length > 0 && (
        <Pagination
          data={data}
          page={page}
          size={size}
          onPageChange={setPage}
          onSizeChange={setSize}
          sizes={sizes}
        />
      )}
    </div>
  )
}
