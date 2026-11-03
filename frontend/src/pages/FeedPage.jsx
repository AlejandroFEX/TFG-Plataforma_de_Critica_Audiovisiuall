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

const TYPES = [
  ['', 'Todo'],
  ['MOVIE', 'Películas'],
  ['TV', 'Series'],
]

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

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="label-caps mb-1 block">{label}</span>
      {children}
    </label>
  )
}

export default function FeedPage() {
  const { user } = useAuth()
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [showFilters, setShowFilters] = useState(false)

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
      <div className="page py-16 sm:py-24">
        <p className="label-caps mb-4">Descubrimiento social de cine y series</p>
        <h1 className="max-w-xl text-4xl font-semibold leading-[1.1] sm:text-5xl">
          Lo mejor suele ser lo que nadie te ha recomendado.
        </h1>
        <p className="mt-5 max-w-lg text-ink-soft">
          Aquí no se puntúan las películas, sino las recomendaciones. Los críticos ganan
          reputación cuando descubren a otros algo poco conocido que merece la pena.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/register" className="btn btn-primary !px-5 !py-2">Crear cuenta</Link>
          <Link to="/ranking" className="btn btn-secondary !px-5 !py-2">Ver el ranking</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="mb-5 flex items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold">Tu feed</h1>
          {data && (
            <p className="mt-0.5 text-xs text-ink-faint">
              {data.length} {data.length === 1 ? 'recomendación' : 'recomendaciones'}
            </p>
          )}
        </div>
        <div className="w-44">
          <select
            value={filters.sortBy}
            onChange={e => update('sortBy', e.target.value)}
            aria-label="Ordenar por"
            className="input"
          >
            <option value="recent">Más recientes</option>
            <option value="oldest">Más antiguas</option>
            <option value="criticPoints">Mejores críticos (CP)</option>
            <option value="rating">Mejor valoradas</option>
            <option value="leastPopular">Menos populares</option>
          </select>
        </div>
      </div>

      {/* Tipo + botón de filtros */}
      <div className="mb-3 flex items-center justify-between gap-3">
        <div className="inline-flex rounded-md border border-line bg-surface p-0.5">
          {TYPES.map(([value, label]) => (
            <button
              key={value}
              onClick={() => update('mediaType', value)}
              className={`rounded px-3 py-1 text-sm transition-colors ${
                filters.mediaType === value
                  ? 'bg-ink text-white'
                  : 'text-ink-soft hover:text-ink'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        <button
          onClick={() => setShowFilters(s => !s)}
          className="btn btn-secondary"
          aria-expanded={showFilters}
        >
          Filtros{activeCount > 0 && ` · ${activeCount}`}
        </button>
      </div>

      {showFilters && (
        <div className="panel mb-5 p-4">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <Field label="Género">
              <select
                value={filters.genre}
                onChange={e => update('genre', e.target.value)}
                className="input"
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
                className="input"
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
                className="input"
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
                className="input"
              />
            </Field>

            <Field label="Publicada hasta">
              <input
                type="date"
                value={filters.to}
                min={filters.from || undefined}
                onChange={e => update('to', e.target.value)}
                className="input"
              />
            </Field>
          </div>

          {activeCount > 0 && (
            <div className="mt-4 text-right">
              <button
                onClick={() => setFilters(EMPTY_FILTERS)}
                className="text-sm text-gem hover:underline"
              >
                Limpiar filtros
              </button>
            </div>
          )}
        </div>
      )}

      {isLoading && (
        <p className="py-16 text-center text-sm text-ink-faint">Cargando…</p>
      )}

      {!isLoading && data?.length === 0 && activeCount > 0 && (
        <p className="py-16 text-center text-sm text-ink-soft">
          Ninguna recomendación coincide con estos filtros.
        </p>
      )}

      {!isLoading && data?.length === 0 && activeCount === 0 && (
        <div className="py-16 text-center">
          <p className="mb-3 text-ink-soft">
            Tu feed está vacío. Sigue a otros críticos para ver sus recomendaciones.
          </p>
          <Link to="/ranking" className="text-sm font-medium text-gem hover:underline">
            Ver el ranking de críticos →
          </Link>
        </div>
      )}

      {data?.length > 0 && (
        <div className="panel divide-y divide-line">
          {data.map(rec => (
            <RecommendationCard key={rec.id} rec={rec} onRated={refetch} />
          ))}
        </div>
      )}
    </div>
  )
}
