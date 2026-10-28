import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getRanking } from '../api/auth'
import { follow, isFollowing } from '../api/social'
import { useAuth } from '../context/AuthContext'

export default function RankingPage() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const { data, isLoading } = useQuery({
    queryKey: ['ranking'],
    queryFn: () => getRanking().then(r => r.data),
  })

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-lg font-semibold text-gray-900 mb-6">
        Ranking de críticos
      </h1>

      {isLoading && (
        <div className="text-center py-12 text-gray-400 text-sm">
          Cargando...
        </div>
      )}

      <div className="space-y-3">
        {data?.map((critic, index) => (
          <div
            key={critic.id}
            className="bg-white border border-gray-200 rounded-xl p-4 flex items-center gap-4 cursor-pointer hover:border-indigo-200 transition-colors"
            onClick={() => navigate(`/profile/${critic.id}`)}
          >
            <span className="text-2xl font-bold text-gray-200 w-8 text-center">
              {index + 1}
            </span>
            <div className="w-10 h-10 rounded-full bg-indigo-100 flex items-center justify-center text-sm font-semibold text-indigo-700 flex-shrink-0">
              {critic.username.slice(0, 2).toUpperCase()}
            </div>
            <div className="flex-1">
              <p className="font-medium text-gray-900 text-sm">
                {critic.username}
              </p>
              {critic.bio && (
                <p className="text-xs text-gray-400 truncate">{critic.bio}</p>
              )}
            </div>
            <div className="text-right">
              <p className="text-sm font-semibold text-indigo-600">
                {critic.criticPoints.toFixed(2)}
              </p>
              <p className="text-xs text-gray-400">Critic Points</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}