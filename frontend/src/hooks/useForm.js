import { useState } from 'react'

/**
 * Formularios con validación bajo cada input (mismo comportamiento que Login y Registro):
 * el error aparece al escribir o salir del campo y, para todos, al intentar enviar.
 * `rules` = { campo: (valor, valores) => 'mensaje' | '' }
 */
export default function useForm(initialValues, rules) {
  const [values, setValues] = useState(initialValues)
  const [touched, setTouched] = useState({})
  const [serverErrors, setServerErrors] = useState({})

  const errors = {}
  for (const [name, rule] of Object.entries(rules)) {
    const message = rule(values[name], values)
    if (message) errors[name] = message
  }

  const setValue = (name, value) => {
    setValues((v) => ({ ...v, [name]: value }))
    setTouched((t) => ({ ...t, [name]: true }))
    setServerErrors((s) => (s[name] ? { ...s, [name]: '' } : s))
  }

  // Props listas para <Input {...form.field('nombre')} />
  const field = (name) => ({
    name,
    value: values[name] ?? '',
    onChange: (e) => setValue(name, e.target.value),
    onBlur: () => setTouched((t) => ({ ...t, [name]: true })),
    error: (touched[name] ? errors[name] : '') || serverErrors[name] || '',
  })

  // Marca todo como tocado y dice si el formulario es válido
  const validateAll = () => {
    setTouched(Object.fromEntries(Object.keys(rules).map((name) => [name, true])))
    return Object.keys(errors).length === 0
  }

  return { values, field, setValue, errors, setServerErrors, validateAll }
}
