import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { getFeed } from '../api/social'
import RecommendationCard from '../components/RecommendationCard'
import { Link } from 'react-router-dom'

export default function FeedPage() {
  const { user } = useAuth()
  const [mediaType, setMediaType] = useState('')
  const [maxPopularity, setMaxPopularity] = useState('')

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['feed', mediaType, maxPopularity],
    queryFn: () => getFeed({
      mediaType: mediaType || undefined,
      maxPopularity: maxPopularity || undefined,
    }).then(r => r.data),
    enabled: !!user,
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
        <div className="flex items-center gap-2">
          <select
            value={mediaType}
            onChange={e => setMediaType(e.target.value)}
            className="text-xs border border-gray-200 rounded-lg px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            <option value="">Todo</option>
            <option value="MOVIE">Películas</option>
            <option value="TV">Series</option>
          </select>
          <select
            value={maxPopularity}
            onChange={e => setMaxPopularity(e.target.value)}
            className="text-xs border border-gray-200 rounded-lg px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            <option value="">Cualquier popularidad</option>
            <option value="20">Solo joyas ocultas (&lt;20)</option>
            <option value="50">Poco conocidas (&lt;50)</option>
          </select>
        </div>
      </div>

      {isLoading && (
        <div className="text-center py-12 text-gray-400 text-sm">
          Cargando...
        </div>
      )}

      {!isLoading && data?.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500 text-sm mb-4">
            Tu feed está vacío. Sigue a otros críticos para ver sus recomendaciones.
          </p>
          <Link
            to="/ranking"
            className="text-indigo-600 text-sm hover:underline"
          >
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