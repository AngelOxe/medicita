import { useCallback, useEffect, useState } from 'react'

/**
 * Listado con paginación y filtros DEL SERVIDOR.
 * Cada cambio de página, tamaño o filtro hace una petición nueva con ?page=&size=&filtros.
 * El backend devuelve solo la página pedida: aquí nunca se corta ni se filtra nada.
 *
 * `fetcher` debe ser una función estable (definida fuera del componente).
 */
export default function usePaginatedList(fetcher, filters = {}, initialSize = 10) {
  const filtersKey = JSON.stringify(filters)
  const [pageState, setPageState] = useState({ page: 0, filtersKey })
  const [size, setSizeState] = useState(initialSize)
  const [reloadCount, setReloadCount] = useState(0)
  const [result, setResult] = useState({ key: null, data: null, error: null })

  // Si cambia un filtro, se vuelve a la primera página
  const page = pageState.filtersKey === filtersKey ? pageState.page : 0
  const requestKey = `${page}|${size}|${filtersKey}|${reloadCount}`

  useEffect(() => {
    let active = true
    fetcher({ page, size, ...JSON.parse(filtersKey) })
      .then((data) => {
        if (active) setResult({ key: requestKey, data, error: null })
      })
      .catch((error) => {
        // Se conserva la última página buena para no vaciar la tabla por un error pasajero
        if (active) setResult((prev) => ({ key: requestKey, data: prev.data, error }))
      })
    return () => {
      active = false
    }
  }, [fetcher, page, size, filtersKey, requestKey])

  const setPage = useCallback((p) => setPageState({ page: p, filtersKey }), [filtersKey])
  const setSize = useCallback(
    (s) => {
      setSizeState(s)
      setPageState({ page: 0, filtersKey })
    },
    [filtersKey],
  )
  const reload = useCallback(() => setReloadCount((n) => n + 1), [])

  return {
    data: result.data,
    error: result.error,
    loading: result.key !== requestKey,
    page,
    setPage,
    size,
    setSize,
    reload,
  }
}
