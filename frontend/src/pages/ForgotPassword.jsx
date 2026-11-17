import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, Mail, MailCheck } from 'lucide-react'
import AuthShell from '../components/auth/AuthShell'
import Input from '../components/ui/Input'
import Button from '../components/ui/Button'
import { forgotPassword } from '../services/authService'
import { getErrorMessage, getFieldErrors } from '../lib/apiErrors'
import { validateEmail } from '../lib/validators'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [touched, setTouched] = useState(false)
  const [serverError, setServerError] = useState('')
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  const error = touched ? validateEmail(email) || serverError : ''

  const handleSubmit = async (e) => {
    e.preventDefault()
    setTouched(true)
    if (validateEmail(email)) return

    setLoading(true)
    setFormError('')
    try {
      await forgotPassword(email.trim())
      setSent(true)
    } catch (err) {
      const fields = getFieldErrors(err)
      if (fields.email) setServerError(fields.email)
      else setFormError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Recuperar contraseña"
      subtitle="Te enviaremos un enlace para crear una nueva contraseña."
    >
      {sent ? (
        <div className="space-y-4 text-center">
          <MailCheck size={40} className="mx-auto text-ok" />
          <p className="text-sm text-ink">
            Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.
          </p>
          <Link to="/login" className="inline-block text-sm font-medium text-brand-600 hover:underline">
            Volver a iniciar sesión
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
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
              setTouched(true)
              setServerError('')
              setFormError('')
            }}
            onBlur={() => setTouched(true)}
            error={error}
          />

          {formError && (
            <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
              {formError}
            </p>
          )}

          <Button type="submit" loading={loading} className="w-full">
            Enviar enlace
          </Button>

          <Link
            to="/login"
            className="flex items-center justify-center gap-1 text-sm text-muted hover:text-ink"
          >
            <ArrowLeft size={16} /> Volver a iniciar sesión
          </Link>
        </form>
      )}
    </AuthShell>
  )
}