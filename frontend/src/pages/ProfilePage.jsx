import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getProfile } from '../api/auth'
import { getRecommendationsByUser } from '../api/recommendations'
import { follow, unfollow, isFollowing } from '../api/social'
import { useAuth } from '../context/AuthContext'
import RecommendationCard from '../components/RecommendationCard'

export default function ProfilePage() {
  const { id } = useParams()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const userId = parseInt(id)

  const { data: profile, isLoading: loadingProfile } = useQuery({
    queryKey: ['profile', userId],
    queryFn: () => getProfile(userId).then(r => r.data),
  })

  const { data: recommendations, isLoading: loadingRecs, refetch } = useQuery({
    queryKey: ['recommendations', userId],
    queryFn: () => getRecommendationsByUser(userId).then(r => r.data),
  })

  const { data: following, refetch: refetchFollowing } = useQuery({
    queryKey: ['following', userId],
    queryFn: () => isFollowing(userId).then(r => r.data),
    enabled: !!user && user.id !== userId,
  })

  async function handleFollow() {
    try {
      if (following) {
        await unfollow(userId)
      } else {
        await follow(userId)
      }
      refetchFollowing()
      queryClient.invalidateQueries(['feed'])
    } catch (err) {
      console.error(err)
    }
  }

  if (loadingProfile) {
    return (
      <div className="text-center py-12 text-gray-400 text-sm">Cargando...</div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <div className="bg-white border border-gray-200 rounded-xl p-6 mb-6">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-full bg-indigo-100 flex items-center justify-center text-xl font-bold text-indigo-700">
              {profile?.username.slice(0, 2).toUpperCase()}
            </div>
            <div>
              <h1 className="text-lg font-semibold text-gray-900">
                {profile?.username}
              </h1>
              {profile?.bio && (
                <p className="text-sm text-gray-500 mt-1">{profile.bio}</p>
              )}
              <p className="text-sm font-medium text-indigo-600 mt-1">
                {profile?.criticPoints.toFixed(2)} Critic Points
              </p>
            </div>
          </div>
          {user && user.id !== userId && (
            <button
              onClick={handleFollow}
              className={`text-sm px-4 py-2 rounded-lg font-medium transition-colors ${
                following
                  ? 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  : 'bg-indigo-600 text-white hover:bg-indigo-700'
              }`}
            >
              {following ? 'Siguiendo' : 'Seguir'}
            </button>
          )}
        </div>
      </div>

      <h2 className="text-sm font-semibold text-gray-700 mb-3">
        Recomendaciones ({recommendations?.length ?? 0})
      </h2>

      {loadingRecs && (
        <div className="text-center py-8 text-gray-400 text-sm">Cargando...</div>
      )}

      <div className="space-y-4">
        {recommendations?.map(rec => (
          <RecommendationCard key={rec.id} rec={rec} onRated={refetch} />
        ))}
      </div>

      {!loadingRecs && recommendations?.length === 0 && (
        <p className="text-center text-gray-400 text-sm py-8">
          Este usuario aún no ha publicado recomendaciones.
        </p>
      )}
    </div>
  )
}