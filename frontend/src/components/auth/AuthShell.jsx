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

      <aside className="hidden flex-col items-center justify-center gap-6 bg-gradient-to-br from-[#14465b] to-sidebar-deep p-12 text-white lg:flex">
        <img src="/logo.svg" alt="Logo de MediCita" className="h-56 w-56" />
        <p className="text-5xl font-extrabold tracking-[0.2em]">MEDICITA</p>
      </aside>
    </div>
  )
}