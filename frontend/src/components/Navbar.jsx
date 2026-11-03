import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { getProfile } from '../api/auth'
import Avatar from './Avatar'

const linkClass = ({ isActive }) =>
  isActive ? 'font-medium text-ink' : 'text-ink-soft transition-colors hover:text-ink'

export default function Navbar() {
  const { user, logoutUser } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  // Misma clave que ProfilePage: al subir una foto nueva, la barra se actualiza sola
  const { data: me } = useQuery({
    queryKey: ['profile', user?.id],
    queryFn: () => getProfile(user.id).then(r => r.data),
    enabled: !!user,
  })

  function handleLogout() {
    logoutUser()
    queryClient.clear() // la siguiente sesión no debe ver datos en caché de la anterior
    navigate('/login')
  }

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-paper/90 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-3xl items-center justify-between px-4">
        <Link
          to="/feed"
          className="flex items-center gap-2 font-serif text-xl font-semibold tracking-tight"
        >
          <span aria-hidden className="h-2.5 w-2.5 rounded-full bg-gem" />
          Fictionary
        </Link>

        <nav className="flex items-center gap-5 text-sm">
          {user && <NavLink to="/feed" className={linkClass}>Feed</NavLink>}
          <NavLink to="/search" className={linkClass}>Buscar</NavLink>
          <NavLink to="/ranking" className={linkClass}>Ranking</NavLink>

          {user ? (
            <>
              <Link
                to={`/profile/${user.id}`}
                title="Tu perfil"
                className="flex items-center gap-2"
              >
                <Avatar src={me?.avatarUrl} name={user.username} size="sm" />
                <span className="hidden text-ink-soft sm:inline">{user.username}</span>
              </Link>
              <button
                onClick={handleLogout}
                className="text-ink-soft transition-colors hover:text-ink"
              >
                Salir
              </button>
            </>
          ) : (
            <Link to="/login" className="btn btn-primary">Entrar</Link>
          )}
        </nav>
      </div>
    </header>
  )
}
