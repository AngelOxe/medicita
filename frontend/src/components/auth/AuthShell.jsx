import { CalendarCheck } from 'lucide-react'

export default function AuthShell({ title, subtitle, children }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="flex flex-col px-6 py-8 sm:px-12">
        <span className="text-xl font-bold text-brand-700">MediCita</span>

        <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-10">
          <h1 className="text-2xl font-bold">{title}</h1>
          <p className="mb-6 mt-1 text-sm text-muted">{subtitle}</p>
          {children}
        </div>

        <p className="text-xs text-muted">MediCita © 2026</p>
      </div>

      <aside className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-[#14465b] to-sidebar-deep p-12 text-white lg:flex">
        <span className="w-fit rounded-full bg-white/10 px-3 py-1 text-xs font-semibold tracking-wider">
          HECHO PARA TU CONSULTORIO
        </span>

        <div>
          <div className="mb-8 w-64 rounded-xl bg-white/10 p-4 backdrop-blur">
            <div className="flex items-center gap-3">
              <CalendarCheck size={28} />
              <div>
                <p className="text-sm font-semibold">Tu agenda, al día</p>
                <p className="text-xs text-white/70">12 citas para hoy</p>
              </div>
            </div>
          </div>
          <h2 className="text-4xl font-bold leading-tight">
            Tu consultorio,
            <br />
            mejor organizado
          </h2>
          <p className="mt-3 max-w-md text-white/80">
            Gestiona citas, pacientes y horarios desde un solo lugar.
          </p>
        </div>

        <p className="text-sm text-white/70">
          Más tiempo para lo que importa: tus pacientes.
        </p>
      </aside>
    </div>
  )
}