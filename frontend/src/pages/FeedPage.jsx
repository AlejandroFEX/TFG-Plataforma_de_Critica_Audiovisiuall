import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getFeed } from '../api/social'
import { getGenres } from '../api/catalog'
import RecommendationCard from '../components/RecommendationCard'

const EMPTY_FILTERS = {
  mediaType: '',
  genre: '',
  popularity: '',
  minRating: '',
  from: '',
  to: '',
  sortBy: 'recent',
}

function toParams(f) {
  const params = { sortBy: f.sortBy }
  if (f.mediaType) params.mediaType = f.mediaType
  if (f.genre) params.genre = f.genre
  if (f.popularity === 'gems') params.maxPopularity = 20
  if (f.popularity === 'low') params.maxPopularity = 50
  if (f.popularity === 'high') params.minPopularity = 50
  if (f.minRating) params.minRating = f.minRating
  if (f.from) params.from = f.from
  if (f.to) params.to = f.to
  return params
}

const inputClass =
  'w-full text-sm border border-gray-200 rounded-lg px-2.5 py-1.5 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500'

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="block text-xs text-gray-500 mb-1">{label}</span>
      {children}
    </label>
  )
}

export default function FeedPage() {
  const { user } = useAuth()
  const [filters, setFilters] = useState(EMPTY_FILTERS)

  const update = (key, value) => setFilters(prev => ({ ...prev, [key]: value }))

  const activeCount = Object.entries(filters).filter(
    ([key, value]) => key !== 'sortBy' && value !== ''
  ).length

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['feed', filters],
    queryFn: () => getFeed(toParams(filters)).then(r => r.data),
    enabled: !!user,
    placeholderData: keepPreviousData,
  })

  const { data: genres } = useQuery({
    queryKey: ['genres'],
    queryFn: () => getGenres().then(r => r.data),
    staleTime: 60_000,
  })

  if (!user) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 text-center">
        <h2 className="text-xl font-semibold text-gray-800 mb-3">
          Bienvenido a Fictionary
        </h2>
        <p className="text-gray-500 mb-6">
          Descubre contenido audiovisual poco popular recomendado por curadores.
        </p>
        <Link
          to="/register"
          className="bg-indigo-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-indigo-700"
        >
          Crear cuenta
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold text-gray-900">Tu feed</h1>
        <div className="w-48">
          <select
            value={filters.sortBy}
            onChange={e => update('sortBy', e.target.value)}
            className={inputClass}
          >
            <option value="recent">Más recientes</option>
            <option value="oldest">Más antiguas</option>
            <option value="criticPoints">Mejores críticos (CP)</option>
            <option value="rating">Mejor valoradas</option>
            <option value="leastPopular">Menos populares</option>
          </select>
        </div>
      </div>

      {/* Filtros */}
      <div className="bg-white border border-gray-200 rounded-xl p-4 mb-5">
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <Field label="Tipo">
            <select
              value={filters.mediaType}
              onChange={e => update('mediaType', e.target.value)}
              className={inputClass}
            >
              <option value="">Todo</option>
              <option value="MOVIE">Películas</option>
              <option value="TV">Series</option>
            </select>
          </Field>

          <Field label="Género">
            <select
              value={filters.genre}
              onChange={e => update('genre', e.target.value)}
              className={inputClass}
            >
              <option value="">Todos</option>
              {genres?.map(g => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
          </Field>

          <Field label="Popularidad">
            <select
              value={filters.popularity}
              onChange={e => update('popularity', e.target.value)}
              className={inputClass}
            >
              <option value="">Cualquiera</option>
              <option value="gems">Joyas ocultas (&lt; 20)</option>
              <option value="low">Poco conocidas (&lt; 50)</option>
              <option value="high">Populares (≥ 50)</option>
            </select>
          </Field>

          <Field label="Valoración mínima">
            <select
              value={filters.minRating}
              onChange={e => update('minRating', e.target.value)}
              className={inputClass}
            >
              <option value="">Cualquiera</option>
              <option value="5">5 o más</option>
              <option value="7">7 o más</option>
              <option value="8">8 o más</option>
              <option value="9">9 o más</option>
            </select>
          </Field>

          <Field label="Publicada desde">
            <input
              type="date"
              value={filters.from}
              max={filters.to || undefined}
              onChange={e => update('from', e.target.value)}
              className={inputClass}
            />
          </Field>

          <Field label="Publicada hasta">
            <input
              type="date"
              value={filters.to}
              min={filters.from || undefined}
              onChange={e => update('to', e.target.value)}
              className={inputClass}
            />
          </Field>
        </div>

        {activeCount > 0 && (
          <div className="mt-3 flex items-center justify-between">
            <span className="text-xs text-gray-400">
              {activeCount} {activeCount === 1 ? 'filtro activo' : 'filtros activos'}
            </span>
            <button
              onClick={() => setFilters(EMPTY_FILTERS)}
              className="text-xs text-indigo-600 hover:text-indigo-800"
            >
              Limpiar filtros
            </button>
          </div>
        )}
      </div>

      {isLoading && (
        <div className="text-center py-12 text-gray-400 text-sm">Cargando...</div>
      )}

      {!isLoading && data?.length === 0 && activeCount > 0 && (
        <div className="text-center py-12 text-gray-500 text-sm">
          Ninguna recomendación coincide con estos filtros.
        </div>
      )}

      {!isLoading && data?.length === 0 && activeCount === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500 text-sm mb-4">
            Tu feed está vacío. Sigue a otros críticos para ver sus recomendaciones.
          </p>
          <Link to="/ranking" className="text-indigo-600 text-sm hover:underline">
            Ver ranking de críticos →
          </Link>
        </div>
      )}

      <div className="space-y-4">
        {data?.map(rec => (
          <RecommendationCard key={rec.id} rec={rec} onRated={refetch} />
        ))}
      </div>
    </div>
  )
}
