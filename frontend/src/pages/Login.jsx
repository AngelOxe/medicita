import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Mail } from 'lucide-react'
import AuthShell from '../components/auth/AuthShell'
import GoogleButton from '../components/auth/GoogleButton'
import PasswordInput from '../components/auth/PasswordInput'
import Input from '../components/ui/Input'
import Button from '../components/ui/Button'
import { useAuth } from '../context/useAuth'
import { getErrorMessage, getFieldErrors } from '../lib/apiErrors'
import { homeByRole } from '../lib/roles'
import { validateEmail } from '../lib/validators'

const validatePass = (v) => (v ? '' : 'La contraseña es obligatoria.')

export default function Login() {
  const { user, login, loginWithGoogle } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [values, setValues] = useState({ email: '', password: '' })
  const [touched, setTouched] = useState({})
  const [serverErrors, setServerErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)

  if (user) return <Navigate to={homeByRole[user.rol] || '/'} replace />

  const rules = { email: validateEmail, password: validatePass }
  const errors = {
    email: touched.email ? rules.email(values.email) || serverErrors.email : '',
    password: touched.password ? rules.password(values.password) || serverErrors.password : '',
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setValues((v) => ({ ...v, [name]: value }))
    setTouched((t) => ({ ...t, [name]: true }))
    setServerErrors((s) => ({ ...s, [name]: '' }))
    setFormError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setTouched({ email: true, password: true })
    if (rules.email(values.email) || rules.password(values.password)) return

    setLoading(true)
    setFormError('')
    try {
      const usuario = await login(values.email.trim(), values.password)
      const destino = location.state?.from?.pathname || homeByRole[usuario.rol] || '/'
      navigate(destino, { replace: true })
    } catch (error) {
      const fields = getFieldErrors(error)
      if (Object.keys(fields).length) setServerErrors(fields)
      else setFormError(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  const handleGoogle = async (credential) => {
    setFormError('')
    try {
      const usuario = await loginWithGoogle(credential)
      navigate(location.state?.from?.pathname || homeByRole[usuario.rol] || '/', { replace: true })
    } catch (error) {
      setFormError(getErrorMessage(error))
    }
  }

  return (
    <AuthShell title="Iniciar sesión" subtitle="Ingresa tus datos para acceder a tu cuenta.">
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <Input
          label="Correo electrónico"
          name="email"
          type="email"
          icon={Mail}
          autoComplete="off"
          placeholder="correo@ejemplo.com"
          value={values.email}
          onChange={handleChange}
          onBlur={() => setTouched((t) => ({ ...t, email: true }))}
          error={errors.email}
        />
        <PasswordInput
          label="Contraseña"
          name="password"
          autoComplete="new-password"
          placeholder="Tu contraseña"
          value={values.password}
          onChange={handleChange}
          onBlur={() => setTouched((t) => ({ ...t, password: true }))}
          error={errors.password}
        />

        <div className="text-right">
          <Link to="/recuperar" className="text-sm text-brand-600 hover:underline">
            ¿Olvidaste tu contraseña?
          </Link>
        </div>

        {formError && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {formError}
          </p>
        )}

        <Button type="submit" loading={loading} className="w-full">
          Iniciar sesión
        </Button>

        <GoogleButton onCredential={handleGoogle} onError={setFormError} />

        <p className="text-center text-sm text-muted">
          ¿No tienes cuenta?{' '}
          <Link to="/registro" className="font-medium text-brand-600 hover:underline">
            Regístrate
          </Link>
        </p>
      </form>
    </AuthShell>
  )
}