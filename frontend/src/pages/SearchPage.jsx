import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchCatalog, getGenres } from '../api/catalog'
import { publishRecommendation } from '../api/recommendations'
import { useAuth } from '../context/AuthContext'
import useDebounce from '../hooks/useDebounce'
import UserSearch from '../components/UserSearch'

const inputClass =
  'text-sm border border-gray-200 rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500'

export default function SearchPage() {
  const { user } = useAuth()
  const [tab, setTab] = useState('content') // 'content' | 'users'
  const [query, setQuery] = useState('')
  const [mediaType, setMediaType] = useState('MOVIE')
  const [genre, setGenre] = useState('')
  const [niche, setNiche] = useState(false)
  const [publishing, setPublishing] = useState(null)
  const [text, setText] = useState('')
  const [success, setSuccess] = useState(null)
  const [error, setError] = useState(null)

  const q = useDebounce(query.trim(), 400)
  const hasCriteria = q.length > 0 || genre !== ''

  const { data, isFetching } = useQuery({
    queryKey: ['search', q, mediaType, genre, niche],
    queryFn: () =>
      searchCatalog({ query: q, type: mediaType, genre, niche }).then(r => r.data),
    enabled: tab === 'content' && hasCriteria,
    placeholderData: keepPreviousData,
  })

  const { data: genres } = useQuery({
    queryKey: ['genres', mediaType],
    queryFn: () => getGenres(mediaType).then(r => r.data),
    staleTime: 60 * 60 * 1000,
  })

  const results = hasCriteria ? data : undefined

  function changeType(value) {
    setMediaType(value)
    setGenre('') // los géneros de películas y series no coinciden
  }

  async function handlePublish(title) {
    if (text.trim().length < 10) return
    setError(null)
    setSuccess(null)
    try {
      await publishRecommendation({
        tmdbId: title.tmdbId,
        mediaType: title.mediaType,
        text,
      })
      setSuccess(title.name)
      setPublishing(null)
      setText('')
    } catch (err) {
      setError(err.response?.data?.message || 'Error al publicar')
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-lg font-semibold text-gray-900 mb-4">Buscar</h1>

      {/* Pestañas */}
      <div className="flex gap-1 mb-5 border-b border-gray-200">
        {[['content', 'Contenido'], ['users', 'Usuarios']].map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`px-4 py-2 text-sm font-medium -mb-px border-b-2 transition-colors ${
              tab === key
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'users' && <UserSearch />}

      {tab === 'content' && (
        <>
          <div className="flex flex-col sm:flex-row gap-2 mb-3">
            <select
              value={mediaType}
              onChange={e => changeType(e.target.value)}
              className={inputClass}
            >
              <option value="MOVIE">Películas</option>
              <option value="TV">Series</option>
            </select>
            <select
              value={genre}
              onChange={e => setGenre(e.target.value)}
              className={inputClass}
            >
              <option value="">Todos los géneros</option>
              {genres?.map(g => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
            <input
              type="text"
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder="Título (opcional si eliges género)..."
              className={`flex-1 ${inputClass}`}
            />
          </div>

          {q.length === 0 && genre !== '' && (
            <label className="flex items-center gap-2 text-xs text-gray-600 mb-3">
              <input
                type="checkbox"
                checked={niche}
                onChange={e => setNiche(e.target.checked)}
                className="rounded border-gray-300"
              />
              Mostrar primero las menos populares
            </label>
          )}

          {success && (
            <div className="bg-emerald-50 text-emerald-700 px-4 py-3 rounded-lg mb-4 text-sm">
              Recomendación publicada para {success}
            </div>
          )}

          {error && (
            <div className="bg-red-50 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
              {error}
            </div>
          )}

          {!hasCriteria && (
            <p className="text-center text-gray-400 text-sm py-8">
              Escribe un título o elige un género para empezar.
            </p>
          )}

          {hasCriteria && isFetching && !results && (
            <div className="text-center py-8 text-gray-400 text-sm">Buscando...</div>
          )}

          {hasCriteria && !isFetching && results?.length === 0 && (
            <div className="text-center py-8 text-gray-500 text-sm">
              Sin resultados con estos criterios.
            </div>
          )}

          <div className="space-y-3">
            {results?.map(title => (
              <div
                key={`${title.mediaType}-${title.tmdbId}`}
                className="bg-white border border-gray-200 rounded-xl p-4"
              >
                <div className="flex gap-3">
                  {title.posterUrl && (
                    <img
                      src={title.posterUrl}
                      alt={title.name}
                      className="w-12 h-18 object-cover rounded-lg flex-shrink-0"
                    />
                  )}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <h3 className="font-medium text-gray-900 text-sm [overflow-wrap:anywhere]">
                          {title.name}
                          {title.releaseDate && (
                            <span className="text-gray-400 font-normal">
                              {' '}({title.releaseDate.slice(0, 4)})
                            </span>
                          )}
                        </h3>
                        <p className="text-xs text-gray-400 mt-0.5">
                          Popularidad: {title.popularity?.toFixed(1)}
                          {title.hiddenGem && (
                            <span className="ml-2 text-emerald-600 font-medium">
                              ★ Joya oculta
                            </span>
                          )}
                        </p>
                        {title.genres?.length > 0 && (
                          <div className="flex flex-wrap gap-1 mt-1.5">
                            {title.genres.map(g => (
                              <span
                                key={g}
                                className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full"
                              >
                                {g}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                      {user && (
                        <button
                          onClick={() =>
                            setPublishing(
                              publishing === title.tmdbId ? null : title.tmdbId
                            )
                          }
                          className="flex-shrink-0 text-xs bg-indigo-50 text-indigo-700 px-3 py-1 rounded-lg hover:bg-indigo-100"
                        >
                          Recomendar
                        </button>
                      )}
                    </div>

                    {publishing === title.tmdbId && (
                      <div className="mt-3">
                        <textarea
                          value={text}
                          onChange={e => setText(e.target.value)}
                          maxLength={2000}
                          placeholder="Escribe tu recomendación (mínimo 10 caracteres)..."
                          rows={3}
                          className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500 resize-none"
                        />
                        <button
                          onClick={() => handlePublish(title)}
                          disabled={text.trim().length < 10}
                          className="mt-2 text-xs bg-indigo-600 text-white px-4 py-1.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                        >
                          Publicar
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
