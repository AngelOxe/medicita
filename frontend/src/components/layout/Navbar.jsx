import { LogOut, Menu, UserRound } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'
import { roleLabel } from '../../lib/menu'
import Avatar from '../ui/Avatar'
import ConfirmModal from '../ui/ConfirmModal'

export default function Navbar({ onOpenMenu }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const menuRef = useRef(null)

  // Cierra el menú al hacer clic fuera
  useEffect(() => {
    if (!menuOpen) return
    const onClick = (e) => {
      if (!menuRef.current?.contains(e.target)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [menuOpen])

  async function handleLogout() {
    setLoggingOut(true)
    await logout()
    navigate('/login', { replace: true })
  }

  if (!user) return null

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-line bg-white px-4 sm:px-6">
      <button
        type="button"
        onClick={onOpenMenu}
        aria-label="Abrir menú"
        className="rounded-lg p-2 text-ink hover:bg-surface lg:hidden"
      >
        <Menu size={22} />
      </button>

      <div ref={menuRef} className="relative ml-auto">
        <button
          type="button"
          onClick={() => setMenuOpen((o) => !o)}
          aria-haspopup="menu"
          aria-expanded={menuOpen}
          className="flex items-center gap-3 rounded-lg p-1.5 hover:bg-surface"
        >
          <Avatar nombre={user.nombre} avatarUrl={user.avatarUrl} />
          <span className="hidden text-left sm:block">
            <span className="block text-sm font-semibold leading-tight text-ink">
              {user.nombre}
            </span>
            <span className="block text-xs text-muted">{user.email}</span>
          </span>
        </button>

        {menuOpen && (
          <div
            role="menu"
            className="absolute right-0 mt-2 w-64 rounded-xl border border-line bg-white p-2 shadow-lg"
          >
            <div className="border-b border-line px-3 pb-2 pt-1">
              <p className="text-sm font-semibold text-ink">{user.nombre}</p>
              <p className="text-xs text-muted">{user.email}</p>
              <p className="mt-1 text-xs font-medium text-brand-700">{roleLabel[user.rol]}</p>
            </div>
            <Link
              to="/perfil"
              role="menuitem"
              onClick={() => setMenuOpen(false)}
              className="mt-1 flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-ink hover:bg-surface"
            >
              <UserRound size={16} /> Perfil
            </Link>
            <button
              type="button"
              role="menuitem"
              onClick={() => {
                setMenuOpen(false)
                setConfirmOpen(true)
              }}
              className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-danger hover:bg-danger-bg"
            >
              <LogOut size={16} /> Cerrar sesión
            </button>
          </div>
        )}
      </div>

      <ConfirmModal
        open={confirmOpen}
        title="¿Cerrar sesión?"
        message="Tendrás que iniciar sesión de nuevo para entrar al sistema."
        confirmText="Cerrar sesión"
        cancelText="Volver"
        variant="danger"
        loading={loggingOut}
        onConfirm={handleLogout}
        onCancel={() => setConfirmOpen(false)}
      />
    </header>
  )
}