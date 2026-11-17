import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthShell from '../components/auth/AuthShell'
import PasswordInput from '../components/auth/PasswordInput'
import Button from '../components/ui/Button'
import { useToast } from '../components/ui/useToast'
import { resetPassword } from '../services/authService'
import { getErrorMessage, getFieldErrors } from '../lib/apiErrors'
import { PASSWORD_HELP, validateConfirm, validatePassword } from '../lib/validators'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const navigate = useNavigate()
  const toast = useToast()

  const [values, setValues] = useState({ password: '', confirm: '' })
  const [touched, setTouched] = useState({})
  const [serverError, setServerError] = useState('')
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)

  if (!token) {
    return (
      <AuthShell title="Enlace no válido" subtitle="Falta el token de recuperación.">
        <Link to="/recuperar" className="text-sm font-medium text-brand-600 hover:underline">
          Solicitar un nuevo enlace
        </Link>
      </AuthShell>
    )
  }

  const rules = {
    password: () => validatePassword(values.password),
    confirm: () => validateConfirm(values.password, values.confirm),
  }
  const errorOf = (name) =>
    touched[name] ? rules[name]() || (name === 'password' ? serverError : '') : ''

  const handleChange = (e) => {
    const { name, value } = e.target
    setValues((v) => ({ ...v, [name]: value }))
    setTouched((t) => ({ ...t, [name]: true }))
    setServerError('')
    setFormError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setTouched({ password: true, confirm: true })
    if (rules.password() || rules.confirm()) return

    setLoading(true)
    try {
      await resetPassword(token, values.password)
      toast.success('Contraseña actualizada. Inicia sesión.')
      navigate('/login', { replace: true })
    } catch (err) {
      const fields = getFieldErrors(err)
      if (fields.password) setServerError(fields.password)
      else setFormError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell title="Nueva contraseña" subtitle="Elige una contraseña segura para tu cuenta.">
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <div>
          <PasswordInput label="Nueva contraseña" name="password" autoComplete="new-password"
            placeholder="Nueva contraseña" value={values.password}
            onChange={handleChange} onBlur={() => setTouched((t) => ({ ...t, password: true }))}
            error={errorOf('password')} />
          {!errorOf('password') && <p className="mt-1 text-xs text-muted">{PASSWORD_HELP}</p>}
        </div>
        <PasswordInput label="Confirmar contraseña" name="confirm" autoComplete="new-password"
          placeholder="Repite la contraseña" value={values.confirm}
          onChange={handleChange} onBlur={() => setTouched((t) => ({ ...t, confirm: true }))}
          error={errorOf('confirm')} />

        {formError && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {formError}
          </p>
        )}

        <Button type="submit" loading={loading} className="w-full">
          Guardar contraseña
        </Button>
      </form>
    </AuthShell>
  )
}