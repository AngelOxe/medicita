import { CalendarDays, X } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'
import { menuByRole, sectionLabel } from '../../lib/menu'

export default function Sidebar({ open, onClose }) {
  const { user } = useAuth()
  const items = menuByRole[user?.rol] ?? []

  return (
    <>
      {open && (
        <button
          type="button"
          aria-label="Cerrar menú"
          onClick={onClose}
          className="fixed inset-0 z-30 bg-ink/40 lg:hidden"
        />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-sidebar text-white transition-transform lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex items-center gap-3 px-5 py-5">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-sidebar-active">
            <CalendarDays size={20} />
          </span>
          <span className="text-xl font-bold">MediCita</span>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar menú"
            className="ml-auto lg:hidden"
          >
            <X size={20} />
          </button>
        </div>

        <p className="px-5 pb-2 text-xs font-semibold tracking-wider text-white/60">
          {sectionLabel[user?.rol]}
        </p>
        <nav aria-label="Navegación principal" className="flex flex-col gap-1 px-3">
          {items.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg border-l-4 px-3 py-2.5 text-sm font-medium transition-colors ${
                  isActive
                    ? 'border-brand-600 bg-sidebar-active'
                    : 'border-transparent text-white/80 hover:bg-sidebar-active/60'
                }`
              }
            >
              <Icon size={19} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  )
}