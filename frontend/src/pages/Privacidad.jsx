import { Link } from 'react-router-dom'

const REPOSITORIO = 'https://github.com/AngelOxe/medicita'

function Seccion({ titulo, children }) {
  return (
    <section className="mt-6">
      <h2 className="text-lg font-semibold text-ink">{titulo}</h2>
      <div className="mt-2 space-y-2 text-sm leading-relaxed text-ink">{children}</div>
    </section>
  )
}

/** Aviso de privacidad público. Google lo exige para publicar el inicio de sesión con Google. */
export default function Privacidad() {
  return (
    <div className="min-h-screen bg-surface px-6 py-10">
      <div className="mx-auto max-w-2xl rounded-xl border border-line bg-white p-6 sm:p-8">
        <Link to="/login" className="text-sm font-medium text-brand-600 hover:underline">
          ← Volver a MediCita
        </Link>

        <h1 className="mt-4 text-2xl font-bold text-ink">Aviso de privacidad</h1>
        <p className="mt-1 text-sm text-muted">MediCita · Sistema de gestión de citas médicas · Actualizado en noviembre de 2026</p>

        <Seccion titulo="Quién es responsable">
          <p>
            MediCita es un proyecto académico del Equipo 02 de la materia de Programación Web. Esta aplicación sirve para
            reservar y administrar citas de un consultorio médico.
          </p>
        </Seccion>

        <Seccion titulo="Qué datos recopilamos">
          <ul className="list-disc space-y-1 pl-5">
            <li>Tu nombre y tu correo electrónico.</li>
            <li>Tu teléfono, si decides agregarlo, para enviarte avisos de tus citas.</li>
            <li>Los datos de tus citas (servicio, fecha, hora y estado).</li>
            <li>
              Si inicias sesión con Google: tu nombre, tu correo, tu foto de perfil y un identificador de tu cuenta de
              Google. No recibimos tu contraseña de Google ni acceso a tus archivos, contactos o correos.
            </li>
          </ul>
        </Seccion>

        <Seccion titulo="Para qué los usamos">
          <ul className="list-disc space-y-1 pl-5">
            <li>Identificarte e iniciar tu sesión.</li>
            <li>Reservar, mostrar y cancelar tus citas.</li>
            <li>Enviarte avisos de tus citas por correo, SMS o WhatsApp.</li>
          </ul>
          <p>No vendemos tus datos, no los usamos para publicidad y no los compartimos con terceros, salvo con los servicios necesarios para enviarte los avisos.</p>
        </Seccion>

        <Seccion titulo="Cómo los protegemos">
          <p>
            Las contraseñas se guardan cifradas (nunca en texto plano), la comunicación viaja por HTTPS y cada persona solo
            puede ver sus propios datos según su rol (paciente, médico o administrador).
          </p>
        </Seccion>

        <Seccion titulo="Tus derechos">
          <p>
            Puedes consultar y corregir tu nombre y tu teléfono desde tu perfil. Si quieres que eliminemos tu cuenta,
            solicítalo al responsable del proyecto a través del{' '}
            <a href={REPOSITORIO} target="_blank" rel="noreferrer" className="font-medium text-brand-600 hover:underline">
              repositorio de MediCita
            </a>
            .
          </p>
        </Seccion>
      </div>
    </div>
  )
}
