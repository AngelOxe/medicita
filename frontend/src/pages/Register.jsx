import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Mail, Phone, User } from 'lucide-react'
import AuthShell from '../components/auth/AuthShell'
import GoogleButton from '../components/auth/GoogleButton'
import PasswordInput from '../components/auth/PasswordInput'
import Input from '../components/ui/Input'
import Button from '../components/ui/Button'
import { useToast } from '../components/ui/useToast'
import { useAuth } from '../context/useAuth'
import { homeByRole } from '../lib/roles'
import { register } from '../services/authService'
import { getErrorMessage, getFieldErrors } from '../lib/apiErrors'
import {
  PASSWORD_HELP,
  validateConfirm,
  validateEmail,
  validateNombre,
  validatePassword,
  validateTelefono,
} from '../lib/validators'

const EMPTY = { nombre: '', email: '', telefono: '', password: '', confirm: '' }

export default function Register() {
  const navigate = useNavigate()
  const { show } = useToast()
  const { loginWithGoogle } = useAuth()

  const [values, setValues] = useState(EMPTY)
  const [touched, setTouched] = useState({})
  const [serverErrors, setServerErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)

  const rules = {
    nombre: (v) => validateNombre(v.nombre),
    email: (v) => validateEmail(v.email),
    telefono: (v) => validateTelefono(v.telefono),
    password: (v) => validatePassword(v.password),
    confirm: (v) => validateConfirm(v.password, v.confirm),
  }

  const errorOf = (name) =>
    touched[name] ? rules[name](values) || serverErrors[name] || '' : ''

  const handleChange = (e) => {
    const { name, value } = e.target
    setValues((v) => ({ ...v, [name]: value }))
    setTouched((t) => ({ ...t, [name]: true }))
    setServerErrors((s) => ({ ...s, [name]: '' }))
    setFormError('')
  }

  const blur = (name) => () => setTouched((t) => ({ ...t, [name]: true }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setTouched({ nombre: true, email: true, telefono: true, password: true, confirm: true })
    if (Object.values(rules).some((rule) => rule(values))) return

    setLoading(true)
    setFormError('')
    try {
      await register({
        nombre: values.nombre.trim(),
        email: values.email.trim(),
        telefono: values.telefono.trim(),
        password: values.password,
      })
      show('Cuenta creada. Ya puedes iniciar sesión.', 'success')
      navigate('/login', { replace: true })
    } catch (error) {
      const fields = getFieldErrors(error)
      if (Object.keys(fields).length) setServerErrors(fields)
      else if (error.response?.status === 422) setServerErrors({ email: getErrorMessage(error) })
      else setFormError(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  // Con Google se crea la cuenta (siempre de paciente) y se inicia sesión en el mismo paso
  const handleGoogle = async (credential) => {
    setFormError('')
    try {
      const usuario = await loginWithGoogle(credential)
      show('Sesión iniciada con Google. Completa tu teléfono en tu perfil para recibir avisos.', 'success')
      navigate(homeByRole[usuario.rol] || '/', { replace: true })
    } catch (error) {
      setFormError(getErrorMessage(error))
    }
  }

  return (
    <AuthShell title="Crear cuenta" subtitle="Regístrate para reservar tus citas médicas.">
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <Input label="Nombre completo" name="nombre" icon={User} autoComplete="name"
          placeholder="Tu nombre" value={values.nombre} onChange={handleChange}
          onBlur={blur('nombre')} error={errorOf('nombre')} />
        <Input label="Correo electrónico" name="email" type="email" icon={Mail}
          autoComplete="email" placeholder="correo@ejemplo.com" value={values.email}
          onChange={handleChange} onBlur={blur('email')} error={errorOf('email')} />
        <Input label="Teléfono" name="telefono" type="tel" icon={Phone}
          autoComplete="tel" placeholder="+525512345678" value={values.telefono}
          onChange={handleChange} onBlur={blur('telefono')} error={errorOf('telefono')} />
        <div>
          <PasswordInput label="Contraseña" name="password" autoComplete="new-password"
            placeholder="Crea una contraseña" value={values.password}
            onChange={handleChange} onBlur={blur('password')} error={errorOf('password')} />
          {!errorOf('password') && <p className="mt-1 text-xs text-muted">{PASSWORD_HELP}</p>}
        </div>
        <PasswordInput label="Confirmar contraseña" name="confirm" autoComplete="new-password"
          placeholder="Repite tu contraseña" value={values.confirm}
          onChange={handleChange} onBlur={blur('confirm')} error={errorOf('confirm')} />

        {formError && (
          <p role="alert" className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">
            {formError}
          </p>
        )}

        <Button type="submit" loading={loading} className="w-full">
          Crear cuenta
        </Button>

        <GoogleButton onCredential={handleGoogle} onError={setFormError} />

        <p className="text-center text-sm text-muted">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            Inicia sesión
          </Link>
        </p>
      </form>
    </AuthShell>
  )
}