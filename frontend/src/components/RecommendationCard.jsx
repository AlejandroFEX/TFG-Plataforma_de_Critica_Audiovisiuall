import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { rateRecommendation } from '../api/recommendations'
import Avatar from './Avatar'

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

export default function RecommendationCard({ rec, onRated }) {
  const { user } = useAuth()
  const [score, setScore] = useState('')
  const [rating, setRating] = useState(false)
  const [error, setError] = useState(null)

  const isOwnRecommendation = user?.id === rec.curatorId

  async function handleRate(e) {
    e.preventDefault()
    if (!score) return
    setRating(true)
    setError(null)
    try {
      await rateRecommendation(rec.id, parseInt(score))
      if (onRated) onRated()
    } catch (err) {
      setError(err.response?.data?.message || 'Error al votar')
    } finally {
      setRating(false)
    }
  }

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5 hover:border-indigo-200 transition-colors">
      <div className="flex gap-4">
        {rec.posterUrl && (
          <img
            src={rec.posterUrl}
            alt={rec.titleName}
            className="w-16 h-24 object-cover rounded-lg flex-shrink-0"
          />
        )}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2 mb-1">
            <h3 className="font-semibold text-gray-900 text-base leading-tight [overflow-wrap:anywhere]">
              {rec.titleName}
            </h3>
            {rec.hiddenGem && (
              <span className="flex-shrink-0 text-xs bg-emerald-50 text-emerald-700 border border-emerald-200 px-2 py-0.5 rounded-full font-medium">
                ★ Joya oculta
              </span>
            )}
          </div>
          <div className="flex items-center gap-2 mb-3">
            <span className="text-xs text-gray-500">
              {rec.mediaType === 'MOVIE' ? 'Película' : 'Serie'}
            </span>
            <span className="text-xs text-gray-300">·</span>
            <span className="text-xs text-gray-500">
              Popularidad: {rec.popularitySnapshot.toFixed(1)}
            </span>
          </div>
          <p className="text-sm text-gray-600 leading-relaxed mb-4 whitespace-pre-line [overflow-wrap:anywhere]">
            {rec.text}
          </p>
          <div className="flex items-center justify-between gap-3">
            <Link
              to={`/profile/${rec.curatorId}`}
              className="flex items-center gap-2 min-w-0 group"
            >
              <Avatar src={rec.curatorAvatarUrl} name={rec.curatorUsername} size="sm" />
              <div className="min-w-0">
                <p className="text-xs font-medium text-gray-800 group-hover:text-indigo-600 truncate">
                  {rec.curatorUsername}
                </p>
                <p className="text-xs text-gray-400">
                  {rec.curatorCriticPoints.toFixed(2)} CP · {formatDate(rec.createdAt)}
                </p>
              </div>
            </Link>
            <div className="flex items-center gap-3 flex-shrink-0">
              {rec.ratingCount > 0 && (
                <span className="text-xs text-gray-500">
                  ★ {rec.averageRating.toFixed(1)} ({rec.ratingCount})
                </span>
              )}
              {user && !isOwnRecommendation && (
                <form onSubmit={handleRate} className="flex items-center gap-1">
                  <select
                    value={score}
                    onChange={(e) => setScore(e.target.value)}
                    className="text-xs border border-gray-200 rounded-lg px-2 py-1 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    <option value="">Votar</option>
                    {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((n) => (
                      <option key={n} value={n}>{n}</option>
                    ))}
                  </select>
                  <button
                    type="submit"
                    disabled={!score || rating}
                    className="text-xs bg-indigo-600 text-white px-3 py-1 rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                  >
                    {rating ? '...' : 'OK'}
                  </button>
                </form>
              )}
            </div>
          </div>
          {error && (
            <p className="text-xs text-red-600 mt-2">{error}</p>
          )}
        </div>
      </div>
    </div>
  )
}
