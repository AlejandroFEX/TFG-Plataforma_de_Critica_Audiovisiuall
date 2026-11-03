import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { login } from '../api/auth'

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' })
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
      const { data } = await login(form)
      loginUser({ id: data.userId, username: data.username, email: data.email }, data.token)
      navigate('/feed')
    } catch (err) {
      setError(err.response?.data?.message || 'Credenciales incorrectas')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid min-h-[calc(100vh-3.5rem)] place-items-center px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-1 text-3xl font-semibold">Iniciar sesión</h1>
        <p className="mb-6 text-sm text-ink-soft">
          Entra para ver las recomendaciones de los críticos que sigues.
        </p>

        {error && <div className="alert-error mb-4">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
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
              autoComplete="current-password"
              className="input"
            />
          </label>
          <button type="submit" disabled={loading} className="btn btn-primary w-full !py-2">
            {loading ? 'Entrando…' : 'Entrar'}
          </button>
        </form>

        <p className="mt-5 text-sm text-ink-soft">
          ¿No tienes cuenta?{' '}
          <Link to="/register" className="font-medium text-gem hover:underline">
            Regístrate
          </Link>
        </p>
      </div>
    </div>
  )
}
