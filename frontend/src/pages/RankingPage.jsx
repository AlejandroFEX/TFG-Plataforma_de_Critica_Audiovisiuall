import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getRanking } from '../api/auth'
import Avatar from '../components/Avatar'

export default function RankingPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['ranking'],
    queryFn: () => getRanking().then(r => r.data),
    staleTime: 0, // el ranking cambia con cada voto
  })

  return (
    <div className="page">
      <h1 className="text-3xl font-semibold">Ranking de críticos</h1>
      <p className="mb-6 mt-2 max-w-xl text-sm text-ink-soft">
        Los Critic Points premian a quien recomienda contenido poco conocido y bien
        valorado por los demás. Recomendar un éxito de taquilla suma mucho menos.
      </p>

      {isLoading && (
        <p className="py-16 text-center text-sm text-ink-faint">Cargando…</p>
      )}

      {data?.length === 0 && (
        <p className="py-16 text-center text-sm text-ink-soft">Aún no hay críticos.</p>
      )}

      {data?.length > 0 && (
        <ol className="panel divide-y divide-line">
          {data.map((critic, index) => (
            <li key={critic.id}>
              <Link
                to={`/profile/${critic.id}`}
                className="flex items-center gap-4 p-4 transition-colors hover:bg-paper"
              >
                <span
                  className={`w-8 shrink-0 text-center font-serif text-2xl font-semibold tabular-nums ${
                    index < 3 ? 'text-ink' : 'text-ink-faint/60'
                  }`}
                >
                  {index + 1}
                </span>
                <Avatar src={critic.avatarUrl} name={critic.username} />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium [overflow-wrap:anywhere]">
                    {critic.username}
                  </p>
                  {critic.bio && (
                    <p className="mt-0.5 line-clamp-2 whitespace-pre-line text-xs text-ink-faint [overflow-wrap:anywhere]">
                      {critic.bio}
                    </p>
                  )}
                </div>
                <div className="shrink-0 text-right">
                  <p className="font-serif text-xl font-semibold tabular-nums text-gem">
                    {critic.criticPoints.toFixed(2)}
                  </p>
                  <p className="label-caps">Critic Points</p>
                </div>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}
