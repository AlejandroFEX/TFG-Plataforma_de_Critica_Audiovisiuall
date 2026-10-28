import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { searchCatalog } from '../api/catalog'
import { publishRecommendation } from '../api/recommendations'
import { useAuth } from '../context/AuthContext'

export default function SearchPage() {
  const { user } = useAuth()
  const [query, setQuery] = useState('')
  const [mediaType, setMediaType] = useState('MOVIE')
  const [search, setSearch] = useState('')
  const [publishing, setPublishing] = useState(null)
  const [text, setText] = useState('')
  const [success, setSuccess] = useState(null)
  const [error, setError] = useState(null)

  const { data, isLoading } = useQuery({
    queryKey: ['search', search, mediaType],
    queryFn: () => searchCatalog(search, mediaType).then(r => r.data),
    enabled: search.length > 2,
  })

  function handleSearch(e) {
    e.preventDefault()
    setSearch(query)
  }

  async function handlePublish(title) {
    if (!text.trim()) return
    setError(null)
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
      <h1 className="text-lg font-semibold text-gray-900 mb-4">
        Buscar contenido
      </h1>

      <form onSubmit={handleSearch} className="flex gap-2 mb-6">
        <select
          value={mediaType}
          onChange={e => setMediaType(e.target.value)}
          className="text-sm border border-gray-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        >
          <option value="MOVIE">Películas</option>
          <option value="TV">Series</option>
        </select>
        <input
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="Busca una película o serie..."
          className="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <button
          type="submit"
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700"
        >
          Buscar
        </button>
      </form>

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

      {isLoading && (
        <div className="text-center py-8 text-gray-400 text-sm">Buscando...</div>
      )}

      <div className="space-y-3">
        {data?.map(title => (
          <div
            key={title.tmdbId}
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
              <div className="flex-1">
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-medium text-gray-900 text-sm">
                      {title.name}
                    </h3>
                    <p className="text-xs text-gray-400 mt-0.5">
                      Popularidad: {title.popularity?.toFixed(1)}
                      {title.hiddenGem && (
                        <span className="ml-2 text-emerald-600 font-medium">
                          ★ Joya oculta
                        </span>
                      )}
                    </p>
                  </div>
                  {user && (
                    <button
                      onClick={() => setPublishing(
                        publishing === title.tmdbId ? null : title.tmdbId
                      )}
                      className="text-xs bg-indigo-50 text-indigo-700 px-3 py-1 rounded-lg hover:bg-indigo-100"
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
                      placeholder="Escribe tu recomendación (mínimo 10 caracteres)..."
                      rows={3}
                      className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500 resize-none"
                    />
                    <button
                      onClick={() => handlePublish(title)}
                      disabled={text.length < 10}
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
    </div>
  )
}