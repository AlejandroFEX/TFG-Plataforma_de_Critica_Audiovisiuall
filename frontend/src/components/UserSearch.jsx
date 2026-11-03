import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { searchUsers } from '../api/auth'
import useDebounce from '../hooks/useDebounce'
import Avatar from './Avatar'

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
        placeholder="Busca un usuario por nombre…"
        className="input mb-5"
      />

      {!active && (
        <p className="py-10 text-center text-sm text-ink-faint">
          Escribe al menos 2 caracteres para buscar usuarios.
        </p>
      )}
      {active && isFetching && !results && (
        <p className="py-10 text-center text-sm text-ink-faint">Buscando…</p>
      )}
      {active && !isFetching && results?.length === 0 && (
        <p className="py-10 text-center text-sm text-ink-soft">
          No hay ningún usuario con ese nombre.
        </p>
      )}

      {results?.length > 0 && (
        <ul className="panel divide-y divide-line">
          {results.map(u => (
            <li key={u.id}>
              <Link
                to={`/profile/${u.id}`}
                className="flex items-center gap-3 p-3.5 transition-colors hover:bg-paper"
              >
                <Avatar src={u.avatarUrl} name={u.username} />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium [overflow-wrap:anywhere]">{u.username}</p>
                  {u.bio && (
                    <p className="line-clamp-1 text-xs text-ink-faint [overflow-wrap:anywhere]">
                      {u.bio}
                    </p>
                  )}
                </div>
                <div className="shrink-0 text-right">
                  <p className="font-serif text-lg font-semibold tabular-nums text-gem">
                    {u.criticPoints.toFixed(2)}
                  </p>
                  <p className="label-caps">CP</p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
