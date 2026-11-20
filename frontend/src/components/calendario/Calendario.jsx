import { useEffect, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import Button from '../ui/Button'
import Spinner from '../ui/Spinner'
import { getErrorMessage } from '../../lib/apiErrors'
import {
  diaSemanaISO,
  formatoFecha,
  horaDeMinutos,
  hoyISO,
  lunesDe,
  minutosDe,
  mismoMes,
  partesISO,
  primerDiaMes,
  sumarDias,
  sumarMeses,
} from '../../lib/fechas'
import { listarBloqueos } from '../../services/bloqueoService'
import { listarCitas } from '../../services/citaService'
import { listarHorarios } from '../../services/horarioService'

const PX_HORA = 56 // alto de una hora en la cuadrícula
const VISTAS = [
  { id: 'dia', label: 'Día' },
  { id: 'semana', label: 'Semana' },
  { id: 'mes', label: 'Mes' },
]

// Color de cada cita según su estado (las canceladas no se dibujan)
const COLOR_CITA = {
  confirmada: 'bg-brand-600 text-white',
  completada: 'bg-ok text-white',
  no_asistio: 'bg-warn text-white',
}

const LEYENDA = [
  { clase: 'bg-green-100 border border-green-300', texto: 'Disponible' },
  { clase: 'bg-slate-200 border border-slate-300', texto: 'Sin servicio / bloqueado' },
  { clase: 'bg-brand-600', texto: 'Cita confirmada' },
  { clase: 'bg-ok', texto: 'Completada' },
  { clase: 'bg-warn', texto: 'No asistió' },
]

// Parte de un intervalo que cae en un día concreto: { ini, fin } en minutos, o null si no toca ese día
function recorteDelDia(inicioIso, finIso, fecha) {
  const a = partesISO(inicioIso)
  const b = partesISO(finIso)
  if (fecha < a.fecha || fecha > b.fecha) return null
  const ini = fecha === a.fecha ? a.min : 0
  const fin = fecha === b.fecha ? b.min : 1440
  return fin > ini ? { ini, fin } : null
}

function rangoVisible(vista, fecha) {
  if (vista === 'dia') return { desde: fecha, hasta: fecha }
  if (vista === 'semana') {
    const lunes = lunesDe(fecha)
    return { desde: lunes, hasta: sumarDias(lunes, 6) }
  }
  // Mes: se muestran semanas completas (de lunes a domingo)
  const desde = lunesDe(primerDiaMes(fecha))
  const ultimo = sumarDias(sumarMeses(fecha, 1), -1)
  return { desde, hasta: sumarDias(lunesDe(ultimo), 6) }
}

function tituloDe(vista, fecha) {
  if (vista === 'dia') return formatoFecha(fecha, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
  if (vista === 'mes') return formatoFecha(fecha, { month: 'long', year: 'numeric' })
  const lunes = lunesDe(fecha)
  const domingo = sumarDias(lunes, 6)
  return `${formatoFecha(lunes, { day: 'numeric', month: 'short' })} – ${formatoFecha(domingo, { day: 'numeric', month: 'short', year: 'numeric' })}`
}

/**
 * Calendario del consultorio con vistas de día, semana y mes. Pinta tres cosas:
 * las jornadas de atención (verde), los bloqueos y días sin atención (gris) y las citas (azul).
 * Usa horarios, bloqueos y citas que ya expone la API; no hay endpoints nuevos.
 */
export default function Calendario({ vistaInicial = 'semana' }) {
  const [vista, setVista] = useState(vistaInicial)
  const [fecha, setFecha] = useState(hoyISO())
  const [base, setBase] = useState({ horarios: [], bloqueos: [] })
  const [citasEnRango, setCitasEnRango] = useState({ clave: '', citas: [], error: '' })

  const { desde, hasta } = useMemo(() => rangoVisible(vista, fecha), [vista, fecha])
  const clave = `${desde}|${hasta}`

  // Jornadas y bloqueos cambian poco: se piden una vez
  useEffect(() => {
    Promise.all([
      listarHorarios({ activo: true, size: 100, page: 0 }),
      listarBloqueos({ size: 100, page: 0 }),
    ])
      .then(([h, b]) => setBase({ horarios: h.content, bloqueos: b.content }))
      .catch(() => {})
  }, [])

  // Las citas se piden al servidor cada vez que cambia el rango visible
  useEffect(() => {
    let activo = true
    listarCitas({ desde, hasta, size: 100, page: 0, orden: 'asc' })
      .then((d) => activo && setCitasEnRango({ clave, citas: d.content.filter((c) => c.estado !== 'cancelada'), error: '' }))
      .catch((e) => activo && setCitasEnRango({ clave, citas: [], error: getErrorMessage(e) }))
    return () => {
      activo = false
    }
  }, [desde, hasta, clave])

  const cargando = citasEnRango.clave !== clave
  const { citas, error } = citasEnRango

  const mover = (paso) => {
    if (vista === 'dia') setFecha(sumarDias(fecha, paso))
    else if (vista === 'semana') setFecha(sumarDias(fecha, 7 * paso))
    else setFecha(sumarMeses(fecha, paso))
  }

  const abrirDia = (f) => {
    setFecha(f)
    setVista('dia')
  }

  return (
    <div>
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2">
          <Button variant="secondary" aria-label="Anterior" onClick={() => mover(-1)} className="!px-2.5">
            <ChevronLeft size={16} />
          </Button>
          <Button variant="secondary" aria-label="Siguiente" onClick={() => mover(1)} className="!px-2.5">
            <ChevronRight size={16} />
          </Button>
          <Button variant="secondary" onClick={() => setFecha(hoyISO())}>
            Hoy
          </Button>
          <h2 className="ml-2 text-base font-semibold text-ink first-letter:uppercase">{tituloDe(vista, fecha)}</h2>
        </div>
        <div role="group" aria-label="Vista del calendario" className="inline-flex rounded-lg border border-line bg-white p-0.5">
          {VISTAS.map((v) => (
            <button
              key={v.id}
              type="button"
              aria-pressed={vista === v.id}
              onClick={() => setVista(v.id)}
              className={`rounded-md px-3 py-1.5 text-sm font-medium ${
                vista === v.id ? 'bg-brand-600 text-white' : 'text-muted hover:text-ink'
              }`}
            >
              {v.label}
            </button>
          ))}
        </div>
      </div>

      <ul className="mb-4 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted">
        {LEYENDA.map((l) => (
          <li key={l.texto} className="inline-flex items-center gap-1.5">
            <span className={`h-3 w-3 rounded ${l.clase}`} /> {l.texto}
          </li>
        ))}
      </ul>

      {error && (
        <p role="alert" className="mb-4 rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="relative rounded-xl border border-line bg-white">
        {cargando && (
          <div className="absolute inset-0 z-20 flex items-center justify-center rounded-xl bg-white/60 text-brand-600">
            <Spinner />
          </div>
        )}
        {vista === 'mes' ? (
          <VistaMes fecha={fecha} desde={desde} citas={citas} horarios={base.horarios} onElegirDia={abrirDia} />
        ) : (
          <VistaCuadricula
            dias={vista === 'dia' ? [fecha] : Array.from({ length: 7 }, (_, i) => sumarDias(desde, i))}
            citas={citas}
            horarios={base.horarios}
            bloqueos={base.bloqueos}
            detallado={vista === 'dia'}
            onElegirDia={abrirDia}
          />
        )}
      </div>
    </div>
  )
}

function VistaCuadricula({ dias, citas, horarios, bloqueos, detallado, onElegirDia }) {
  const hoy = hoyISO()

  // Las horas mostradas cubren todas las jornadas y citas visibles (por defecto 8:00 a 18:00)
  const [horaIni, horaFin] = useMemo(() => {
    let ini = 8 * 60
    let fin = 18 * 60
    horarios.forEach((h) => {
      ini = Math.min(ini, minutosDe(h.horaInicio))
      fin = Math.max(fin, minutosDe(h.horaFin))
    })
    citas.forEach((c) => {
      ini = Math.min(ini, partesISO(c.inicio).min)
      fin = Math.max(fin, partesISO(c.fin).min)
    })
    return [Math.floor(ini / 60), Math.min(Math.ceil(fin / 60), 24)]
  }, [horarios, citas])

  const horas = Array.from({ length: horaFin - horaIni }, (_, i) => horaIni + i)
  const alto = horas.length * PX_HORA
  const y = (min) => ((min - horaIni * 60) / 60) * PX_HORA
  const columnas = { gridTemplateColumns: `3.25rem repeat(${dias.length}, minmax(0, 1fr))` }

  return (
    <div className="overflow-x-auto">
      <div className={detallado ? 'min-w-[320px]' : 'min-w-[720px]'}>
        <div className="grid border-b border-line" style={columnas}>
          <div />
          {dias.map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => !detallado && onElegirDia(f)}
              className={`px-2 py-2 text-center text-sm capitalize ${f === hoy ? 'font-bold text-brand-600' : 'font-medium text-ink'}`}
            >
              {formatoFecha(f, { weekday: 'short', day: 'numeric' })}
            </button>
          ))}
        </div>

        <div className="grid" style={columnas}>
          <div className="relative" style={{ height: alto }}>
            {horas.map((h) => (
              <span key={h} className="absolute right-2 -translate-y-1/2 text-xs text-muted" style={{ top: (h - horaIni) * PX_HORA }}>
                {h > horaIni ? horaDeMinutos(h * 60) : ''}
              </span>
            ))}
          </div>

          {dias.map((f) => {
            const jornadas = horarios.filter((h) => h.diaSemana === diaSemanaISO(f))
            return (
              <div key={f} className="relative border-l border-line bg-slate-200/70" style={{ height: alto }}>
                {horas.map((h) => (
                  <div key={h} className="absolute inset-x-0 border-t border-line/70" style={{ top: (h - horaIni) * PX_HORA }} />
                ))}

                {jornadas.map((j) => (
                  <div
                    key={j.id}
                    title={`Atención ${j.horaInicio.slice(0, 5)} – ${j.horaFin.slice(0, 5)}`}
                    className="absolute inset-x-0 border-y border-green-300 bg-green-100"
                    style={{ top: y(minutosDe(j.horaInicio)), height: y(minutosDe(j.horaFin)) - y(minutosDe(j.horaInicio)) }}
                  />
                ))}

                {bloqueos.map((b) => {
                  const r = recorteDelDia(b.inicio, b.fin, f)
                  return r ? (
                    <div
                      key={b.id}
                      title={`Bloqueado: ${b.motivo ?? ''}`}
                      className="absolute inset-x-0 overflow-hidden bg-slate-400/60 px-1 text-[11px] font-medium text-slate-700"
                      style={{ top: y(r.ini), height: y(r.fin) - y(r.ini) }}
                    >
                      {detallado ? `Bloqueado · ${b.motivo ?? ''}` : 'Bloqueado'}
                    </div>
                  ) : null
                })}

                {citas.map((c) => {
                  const r = recorteDelDia(c.inicio, c.fin, f)
                  if (!r) return null
                  const hora = `${horaDeMinutos(r.ini)} – ${horaDeMinutos(r.fin)}`
                  return (
                    <div
                      key={c.id}
                      title={`${hora} · ${c.paciente.nombre} · ${c.servicio.nombre}`}
                      className={`absolute inset-x-0.5 overflow-hidden rounded px-1.5 py-0.5 text-[11px] leading-tight shadow-sm ${COLOR_CITA[c.estado] ?? 'bg-brand-600 text-white'}`}
                      style={{ top: y(r.ini) + 1, height: y(r.fin) - y(r.ini) - 2 }}
                    >
                      <p className="truncate font-semibold">{c.paciente.nombre}</p>
                      {detallado ? <p className="truncate">{hora} · {c.servicio.nombre}</p> : <p className="truncate">{horaDeMinutos(r.ini)}</p>}
                    </div>
                  )
                })}
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

function VistaMes({ fecha, desde, citas, horarios, onElegirDia }) {
  const hoy = hoyISO()
  const semanas = Math.round(
    (Date.parse(sumarDias(rangoVisible('mes', fecha).hasta, 1)) - Date.parse(desde)) / (7 * 86400000),
  )
  const dias = Array.from({ length: semanas * 7 }, (_, i) => sumarDias(desde, i))

  // Cuántas citas hay cada día
  const porDia = useMemo(() => {
    const mapa = {}
    citas.forEach((c) => {
      const f = partesISO(c.inicio).fecha
      mapa[f] = (mapa[f] ?? 0) + 1
    })
    return mapa
  }, [citas])

  return (
    <div className="overflow-x-auto">
      <div className="min-w-[560px]">
        <div className="grid grid-cols-7 border-b border-line text-center text-xs font-semibold uppercase text-muted">
          {['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'].map((d) => (
            <div key={d} className="py-2">{d}</div>
          ))}
        </div>
        <div className="grid grid-cols-7">
          {dias.map((f) => {
            const atiende = horarios.some((h) => h.diaSemana === diaSemanaISO(f))
            const n = porDia[f] ?? 0
            return (
              <button
                key={f}
                type="button"
                onClick={() => onElegirDia(f)}
                className={`flex min-h-24 flex-col items-start gap-1 border-b border-l border-line p-2 text-left transition hover:ring-2 hover:ring-inset hover:ring-brand-200 ${
                  atiende ? 'bg-green-100/70' : 'bg-slate-200/70'
                } ${mismoMes(f, fecha) ? '' : 'opacity-50'}`}
              >
                <span className={`text-sm ${f === hoy ? 'flex h-6 w-6 items-center justify-center rounded-full bg-brand-600 font-bold text-white' : 'font-medium text-ink'}`}>
                  {Number(f.slice(8))}
                </span>
                {n > 0 && (
                  <span className="rounded-full bg-brand-600 px-2 py-0.5 text-xs font-semibold text-white">
                    {n} {n === 1 ? 'cita' : 'citas'}
                  </span>
                )}
              </button>
            )
          })}
        </div>
      </div>
    </div>
  )
}
