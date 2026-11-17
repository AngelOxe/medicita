import { getInitials } from '../../lib/initials'

export default function Avatar({ nombre, avatarUrl, className = '' }) {
  if (avatarUrl) {
    return (
      <img
        src={avatarUrl}
        alt={`Foto de ${nombre}`}
        className={`h-9 w-9 rounded-full object-cover ${className}`}
      />
    )
  }
  return (
    <span
      aria-hidden="true"
      className={`flex h-9 w-9 items-center justify-center rounded-full bg-brand-100 text-sm font-semibold text-brand-700 ${className}`}
    >
      {getInitials(nombre)}
    </span>
  )
}