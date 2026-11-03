import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { register } from '../api/auth'

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const { loginUser } = useAuth()
  const navigate = useNavigate()

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const { data } = await register(form)
      loginUser({ id: data.userId, username: data.username, email: data.email }, data.token)
      navigate('/feed')
    } catch (err) {
      const data = err.response?.data
      // Si el backend devuelve el detalle por campo, se muestra en lugar del mensaje genérico
      setError(
        data?.errors
          ? Object.values(data.errors).join('. ')
          : data?.message || 'Error al registrarse'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid min-h-[calc(100vh-3.5rem)] place-items-center px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-1 text-3xl font-semibold">Crear cuenta</h1>
        <p className="mb-6 text-sm text-ink-soft">
          Recomienda lo que merece la pena y gana reputación cuando otros lo valoran.
        </p>

        {error && <div className="alert-error mb-4">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <label className="block">
            <span className="mb-1 block text-sm font-medium">Nombre de usuario</span>
            <input
              type="text"
              name="username"
              value={form.username}
              onChange={handleChange}
              required
              minLength={3}
              maxLength={30}
              autoComplete="username"
              className="input"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-medium">Email</span>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              required
              autoComplete="email"
              className="input"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-medium">Contraseña</span>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              required
              minLength={8}
              autoComplete="new-password"
              className="input"
            />
            <span className="mt-1 block text-xs text-ink-faint">Mínimo 8 caracteres.</span>
          </label>
          <button type="submit" disabled={loading} className="btn btn-primary w-full !py-2">
            {loading ? 'Creando cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <p className="mt-5 text-sm text-ink-soft">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="font-medium text-gem hover:underline">
            Inicia sesión
          </Link>
        </p>
      </div>
    </div>
  )
}
