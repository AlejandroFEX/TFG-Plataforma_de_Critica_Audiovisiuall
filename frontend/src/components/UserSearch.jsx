import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { searchUsers } from '../api/auth'
import useDebounce from '../hooks/useDebounce'

export default function UserSearch() {
  const [text, setText] = useState('')
  const q = useDebounce(text.trim())
  const active = q.length >= 2

  const { data, isFetching } = useQuery({
    queryKey: ['userSearch', q],
    queryFn: () => searchUsers(q).then(r => r.data),
    enabled: active,
    placeholderData: keepPreviousData,
  })

  const results = active ? data : undefined

  return (
    <div>
      <input
        type="text"
        value={text}
        onChange={e => setText(e.target.value)}
        placeholder="Busca un usuario por nombre..."
        className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500 mb-4"
      />

      {!active && (
        <p className="text-center text-gray-400 text-sm py-8">
          Escribe al menos 2 caracteres para buscar usuarios.
        </p>
      )}

      {active && isFetching && !results && (
        <p className="text-center text-gray-400 text-sm py-8">Buscando...</p>
      )}

      {active && !isFetching && results?.length === 0 && (
        <p className="text-center text-gray-500 text-sm py-8">
          No se ha encontrado ningún usuario con ese nombre.
        </p>
      )}

      <div className="space-y-2">
        {results?.map(u => (
          <Link
            key={u.id}
            to={`/profile/${u.id}`}
            className="bg-white border border-gray-200 rounded-xl p-3 flex items-center gap-3 hover:border-indigo-200 transition-colors"
          >
            {u.avatarUrl ? (
              <img
                src={u.avatarUrl}
                alt={u.username}
                className="w-10 h-10 rounded-full object-cover flex-shrink-0 border border-gray-100"
              />
            ) : (
              <div className="w-10 h-10 rounded-full bg-indigo-100 flex items-center justify-center text-sm font-semibold text-indigo-700 flex-shrink-0">
                {u.username.slice(0, 2).toUpperCase()}
              </div>
            )}
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-gray-900 [overflow-wrap:anywhere]">
                {u.username}
              </p>
              {u.bio && (
                <p className="text-xs text-gray-400 line-clamp-1 [overflow-wrap:anywhere]">
                  {u.bio}
                </p>
              )}
            </div>
            <div className="text-right flex-shrink-0">
              <p className="text-sm font-semibold text-indigo-600">
                {u.criticPoints.toFixed(2)}
              </p>
              <p className="text-xs text-gray-400">CP</p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
