import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { rateRecommendation } from '../api/recommendations'
import Avatar from './Avatar'
import Poster from './Poster'

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

/** Una recomendación como fila de lista: se coloca dentro de un contenedor con `divide-y`. */
export default function RecommendationCard({ rec, onRated }) {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const [myScore, setMyScore] = useState(null)
  const [rating, setRating] = useState(false)
  const [error, setError] = useState(null)

  const isOwn = user?.id === rec.curatorId

  async function submit(score) {
    setRating(true)
    setError(null)
    try {
      await rateRecommendation(rec.id, score)
      setMyScore(score)
      setOpen(false)
      if (onRated) onRated()
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo registrar la valoración')
    } finally {
      setRating(false)
    }
  }

  return (
    <article className="p-5">
      <div className="flex gap-4">
        <Poster url={rec.posterUrl} title={rec.titleName} />

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1">
            <h3 className="font-serif text-lg font-semibold leading-snug [overflow-wrap:anywhere]">
              {rec.titleName}
            </h3>
            {rec.hiddenGem && <span className="chip chip-gem">◆ Joya oculta</span>}
          </div>
          <p className="mt-0.5 text-xs text-ink-faint">
            {rec.mediaType === 'MOVIE' ? 'Película' : 'Serie'} · Popularidad{' '}
            {rec.popularitySnapshot.toFixed(1)}
          </p>

          <p className="mt-3 whitespace-pre-line text-[15px] [overflow-wrap:anywhere]">
            {rec.text}
          </p>

          <div className="mt-4 flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
            <Link
              to={`/profile/${rec.curatorId}`}
              className="group flex min-w-0 items-center gap-2"
            >
              <Avatar src={rec.curatorAvatarUrl} name={rec.curatorUsername} size="sm" />
              <span className="min-w-0 text-xs">
                <span className="block truncate font-medium group-hover:text-gem">
                  {rec.curatorUsername}
                </span>
                <span className="text-ink-faint">
                  {rec.curatorCriticPoints.toFixed(2)} CP · {formatDate(rec.createdAt)}
                </span>
              </span>
            </Link>

            <div className="flex items-center gap-3 text-xs">
              {rec.ratingCount > 0 ? (
                <span className="text-ink-soft">
                  <span className="text-star">★</span>{' '}
                  <span className="font-medium text-ink">{rec.averageRating.toFixed(1)}</span>{' '}
                  · {rec.ratingCount} {rec.ratingCount === 1 ? 'valoración' : 'valoraciones'}
                </span>
              ) : (
                <span className="text-ink-faint">Sin valoraciones</span>
              )}

              {isOwn && <span className="chip">Tu recomendación</span>}

              {!isOwn && user && (
                <button
                  onClick={() => setOpen(o => !o)}
                  className="btn btn-secondary !px-3 !py-1 text-xs"
                >
                  {myScore ? `Tu nota: ${myScore}` : 'Valorar'}
                </button>
              )}

              {!user && (
                <Link to="/login" className="text-gem hover:underline">
                  Inicia sesión para valorar
                </Link>
              )}
            </div>
          </div>

          {open && (
            <div className="mt-3 flex flex-wrap items-center gap-1.5">
              <span className="label-caps mr-1">¿Cuánto te ha servido?</span>
              {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(n => (
                <button
                  key={n}
                  type="button"
                  disabled={rating}
                  onClick={() => submit(n)}
                  className={`h-7 w-7 rounded border text-xs font-medium transition-colors disabled:opacity-50 ${
                    myScore === n
                      ? 'border-ink bg-ink text-white'
                      : 'border-line text-ink-soft hover:border-ink hover:text-ink'
                  }`}
                >
                  {n}
                </button>
              ))}
            </div>
          )}

          {error && <p className="mt-2 text-xs text-danger">{error}</p>}
        </div>
      </div>
    </article>
  )
}
