import { Link, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { getProfile } from '../api/auth'
import Avatar from './Avatar'

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
    queryClient.clear() // evita que la siguiente sesión vea datos en caché de la anterior
    navigate('/login')
  }

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
        <Link to="/feed" className="font-bold text-indigo-600 text-lg">
          Fictionary
        </Link>
        <div className="flex items-center gap-4">
          <Link to="/search" className="text-sm text-gray-600 hover:text-gray-900">
            Buscar
          </Link>
          <Link to="/ranking" className="text-sm text-gray-600 hover:text-gray-900">
            Ranking
          </Link>
          {user ? (
            <>
              <Link
                to={`/profile/${user.id}`}
                className="flex items-center gap-2 text-sm text-gray-700 hover:text-gray-900"
              >
                <Avatar src={me?.avatarUrl} name={user.username} size="sm" />
                <span className="hidden sm:inline">{user.username}</span>
              </Link>
              <button
                onClick={handleLogout}
                className="text-sm text-gray-500 hover:text-gray-700"
              >
                Salir
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="text-sm bg-indigo-600 text-white px-3 py-1.5 rounded-lg hover:bg-indigo-700"
            >
              Entrar
            </Link>
          )}
        </div>
      </div>
    </nav>
  )
}
