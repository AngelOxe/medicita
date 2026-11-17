import { useState } from 'react'
import { Eye, EyeOff, Lock } from 'lucide-react'
import Input from '../ui/Input'

export default function PasswordInput(props) {
  const [visible, setVisible] = useState(false)
  return (
    <Input
      {...props}
      type={visible ? 'text' : 'password'}
      icon={Lock}
      endAdornment={
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          className="rounded p-1.5 text-muted hover:text-ink"
        >
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      }
    />
  )
}