import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Mail, MailCheck } from 'lucide-react'
import AuthShell from '../components/auth/AuthShell'
import Input from '../components/ui/Input'
import Button from '../components/ui/Button'
import { getErrorMessage } from '../lib/apiErrors'
import { validateEmail } from '../lib/validators'
import { solicitarRecuperacion } from '../services/authService'

export default function Recuperar() {
  const [email, setEmail] = useState('')
  const [tocado, setTocado] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [enviado, setEnviado] = useState(false)

  const errorEmail = tocado ? validateEmail(email) : ''

  const enviar = async (e) => {
    e.preventDefault()
    setTocado(true)
    if (validateEmail(email)) return
    setLoading(true)
    setError('')
    try {
      await solicitarRecuperacion(email.trim())
      setEnviado(true)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (enviado) {
    return (
      <AuthShell title="Revisa tu correo" subtitle="Te enviamos las instrucciones.">
        <div className="rounded-xl border border-line bg-white p-5 text-center">
          <MailCheck size={32} className="mx-auto text-brand-600" />
          <p className="mt-3 text-sm text-ink">
            Si el correo <strong>{email.trim()}</strong> está registrado, recibirás un enlace para crear una
            nueva contraseña. El enlace vence en 30 minutos y solo se puede usar una vez.
          </p>
        </div>
        <Link to="/login" className="mt-6 block text-center text-sm font-medium text-brand-600 hover:underline">
          Volver a iniciar sesión
        </Link>
      </AuthShell>
    )
  }

  return (
    <AuthShell title="Recuperar contraseña" subtitle="Escribe tu correo y te enviaremos un enlace para restablecerla.">
      <form onSubmit={enviar} noValidate className="space-y-4">
        <Input
          label="Correo electrónico"
          name="email"
          type="email"
          icon={Mail}
          autoComplete="email"
          placeholder="correo@ejemplo.com"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value)
            setTocado(true)
            setError('')
          }}
          onBlur={() => setTocado(true)}
          error={errorEmail}
        />
        {error && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {error}
          </p>
        )}
        <Button type="submit" loading={loading} className="w-full">
          Enviar enlace
        </Button>
        <p className="text-center text-sm text-muted">
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            Volver a iniciar sesión
          </Link>
        </p>
      </form>
    </AuthShell>
  )
}
