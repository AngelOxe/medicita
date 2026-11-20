import { useState } from 'react'
import Calendario from '../components/calendario/Calendario'
import BloqueosTab from '../components/horarios/BloqueosTab'
import HorariosTab from '../components/horarios/HorariosTab'

const TABS = [
  { id: 'calendario', label: 'Calendario' },
  { id: 'horarios', label: 'Horarios de atención' },
  { id: 'bloqueos', label: 'Bloqueos' },
]

export default function Horarios() {
  const [tab, setTab] = useState('calendario')

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-ink">Horarios</h1>
        <p className="mt-1 text-sm text-muted">
          Jornadas de atención, los servicios que se ofrecen en cada una y los bloqueos de agenda.
        </p>
      </div>

      <div role="tablist" className="mb-4 flex gap-6 border-b border-line">
        {TABS.map((t) => (
          <button
            key={t.id}
            role="tab"
            type="button"
            aria-selected={tab === t.id}
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-2 px-1 pb-3 text-sm font-medium ${
              tab === t.id
                ? 'border-brand-600 text-brand-600'
                : 'border-transparent text-muted hover:text-ink'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'calendario' && <Calendario />}
      {tab === 'horarios' && <HorariosTab />}
      {tab === 'bloqueos' && <BloqueosTab />}
    </div>
  )
}
