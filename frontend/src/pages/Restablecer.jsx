import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthShell from '../components/auth/AuthShell'
import PasswordInput from '../components/auth/PasswordInput'
import Button from '../components/ui/Button'
import { useToast } from '../components/ui/useToast'
import { getErrorMessage, getFieldErrors } from '../lib/apiErrors'
import { PASSWORD_HELP, validateConfirm, validatePassword } from '../lib/validators'
import { restablecerPassword } from '../services/authService'

export default function Restablecer() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const navigate = useNavigate()
  const toast = useToast()

  const [values, setValues] = useState({ password: '', confirm: '' })
  const [touched, setTouched] = useState({})
  const [serverErrors, setServerErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)

  if (!token) {
    return (
      <AuthShell title="Enlace no válido" subtitle="Falta el código de recuperación.">
        <p className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
          Abre el enlace completo que llegó a tu correo, o solicita uno nuevo.
        </p>
        <Link to="/recuperar" className="mt-6 block text-center text-sm font-medium text-brand-600 hover:underline">
          Solicitar un enlace nuevo
        </Link>
      </AuthShell>
    )
  }

  const errors = {
    password: touched.password ? validatePassword(values.password) || serverErrors.password || '' : '',
    confirm: touched.confirm ? validateConfirm(values.password, values.confirm) : '',
  }

  const cambiar = (e) => {
    const { name, value } = e.target
    setValues((v) => ({ ...v, [name]: value }))
    setTouched((t) => ({ ...t, [name]: true }))
    setServerErrors({})
    setFormError('')
  }

  const enviar = async (e) => {
    e.preventDefault()
    setTouched({ password: true, confirm: true })
    if (validatePassword(values.password) || validateConfirm(values.password, values.confirm)) return
    setLoading(true)
    try {
      await restablecerPassword(token, values.password)
      toast.show('Contraseña actualizada. Ya puedes iniciar sesión.', 'success')
      navigate('/login', { replace: true })
    } catch (err) {
      const campos = getFieldErrors(err)
      if (Object.keys(campos).length) setServerErrors(campos)
      else setFormError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell title="Nueva contraseña" subtitle="Crea una contraseña nueva para tu cuenta.">
      <form onSubmit={enviar} noValidate className="space-y-4">
        <PasswordInput
          label="Nueva contraseña"
          name="password"
          autoComplete="new-password"
          value={values.password}
          onChange={cambiar}
          onBlur={() => setTouched((t) => ({ ...t, password: true }))}
          error={errors.password}
        />
        <p className="-mt-2 text-xs text-muted">{PASSWORD_HELP}</p>
        <PasswordInput
          label="Confirmar contraseña"
          name="confirm"
          autoComplete="new-password"
          value={values.confirm}
          onChange={cambiar}
          onBlur={() => setTouched((t) => ({ ...t, confirm: true }))}
          error={errors.confirm}
        />
        {formError && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {formError}{' '}
            <Link to="/recuperar" className="font-medium underline">
              Solicitar otro enlace
            </Link>
          </p>
        )}
        <Button type="submit" loading={loading} className="w-full">
          Guardar contraseña
        </Button>
      </form>
    </AuthShell>
  )
}
