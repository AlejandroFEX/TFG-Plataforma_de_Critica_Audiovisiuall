import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchCatalog, getGenres } from '../api/catalog'
import { publishRecommendation } from '../api/recommendations'
import { useAuth } from '../context/AuthContext'
import useDebounce from '../hooks/useDebounce'
import UserSearch from '../components/UserSearch'
import Poster from '../components/Poster'

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
    <div className="page">
      <h1 className="mb-5 text-3xl font-semibold">Buscar</h1>

      {/* Pestañas */}
      <div className="mb-6 flex gap-6 border-b border-line">
        {[['content', 'Contenido'], ['users', 'Usuarios']].map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`-mb-px border-b-2 pb-2.5 text-sm font-medium transition-colors ${
              tab === key
                ? 'border-ink text-ink'
                : 'border-transparent text-ink-faint hover:text-ink'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'users' && <UserSearch />}

      {tab === 'content' && (
        <>
          <div className="mb-3 grid gap-2 sm:grid-cols-[auto_auto_1fr]">
            <select
              value={mediaType}
              onChange={e => changeType(e.target.value)}
              aria-label="Tipo de contenido"
              className="input"
            >
              <option value="MOVIE">Películas</option>
              <option value="TV">Series</option>
            </select>
            <select
              value={genre}
              onChange={e => setGenre(e.target.value)}
              aria-label="Género"
              className="input"
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
              placeholder="Título (opcional si eliges género)…"
              className="input"
            />
          </div>

          {q.length === 0 && genre !== '' && (
            <label className="mb-3 flex items-center gap-2 text-sm text-ink-soft">
              <input
                type="checkbox"
                checked={niche}
                onChange={e => setNiche(e.target.checked)}
                className="accent-[var(--color-gem)]"
              />
              Mostrar primero las menos populares
            </label>
          )}

          {success && (
            <div className="mb-4 rounded-md bg-gem-soft px-3 py-2 text-sm text-gem">
              Recomendación publicada para «{success}».
            </div>
          )}
          {error && <div className="alert-error mb-4">{error}</div>}

          {!hasCriteria && (
            <p className="py-12 text-center text-sm text-ink-faint">
              Escribe un título o elige un género para empezar.
            </p>
          )}
          {hasCriteria && isFetching && !results && (
            <p className="py-12 text-center text-sm text-ink-faint">Buscando…</p>
          )}
          {hasCriteria && !isFetching && results?.length === 0 && (
            <p className="py-12 text-center text-sm text-ink-soft">
              Sin resultados con estos criterios.
            </p>
          )}

          {results?.length > 0 && (
            <ul className="panel divide-y divide-line">
              {results.map(title => (
                <li key={`${title.mediaType}-${title.tmdbId}`} className="flex gap-4 p-4">
                  <Poster url={title.posterUrl} title={title.name} size="sm" />

                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <h3 className="font-serif text-base font-semibold leading-snug [overflow-wrap:anywhere]">
                          {title.name}
                          {title.releaseDate && (
                            <span className="font-normal text-ink-faint">
                              {' '}{title.releaseDate.slice(0, 4)}
                            </span>
                          )}
                        </h3>
                        <p className="mt-0.5 flex flex-wrap items-center gap-x-2 text-xs text-ink-faint">
                          <span>Popularidad {title.popularity?.toFixed(1)}</span>
                          {title.hiddenGem && <span className="chip chip-gem">◆ Joya oculta</span>}
                        </p>
                        {title.genres?.length > 0 && (
                          <p className="mt-1 text-xs text-ink-soft">
                            {title.genres.join(' · ')}
                          </p>
                        )}
                      </div>
                      {user && (
                        <button
                          onClick={() =>
                            setPublishing(publishing === title.tmdbId ? null : title.tmdbId)
                          }
                          className="btn btn-secondary shrink-0 !px-3 !py-1 text-xs"
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
                          rows={4}
                          placeholder="¿Por qué merece la pena? (mínimo 10 caracteres)…"
                          className="input resize-none"
                        />
                        <div className="mt-2 flex items-center justify-between">
                          <span className="text-xs text-ink-faint">{text.length}/2000</span>
                          <button
                            onClick={() => handlePublish(title)}
                            disabled={text.trim().length < 10}
                            className="btn btn-primary !px-4 !py-1 text-xs"
                          >
                            Publicar
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  )
}
