import { useState, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getProfile, uploadAvatar, updateProfile } from '../api/auth'
import { getRecommendationsByUser } from '../api/recommendations'
import { follow, unfollow, isFollowing, getFollowers, getFollowing } from '../api/social'
import { useAuth } from '../context/AuthContext'
import RecommendationCard from '../components/RecommendationCard'
import UserListModal from '../components/UserListModal'

export default function ProfilePage() {
  const { id } = useParams()
  const { user, loginUser } = useAuth()
  const queryClient = useQueryClient()
  const userId = parseInt(id)
  const isOwn = user?.id === userId
  const fileRef = useRef(null)

  const [editingBio, setEditingBio] = useState(false)
  const [bio, setBio] = useState('')
  const [savingBio, setSavingBio] = useState(false)
  const [uploadingAvatar, setUploadingAvatar] = useState(false)
  const [openList, setOpenList] = useState(null)

  const { data: profile, isLoading: loadingProfile, refetch: refetchProfile } = useQuery({
    queryKey: ['profile', userId],
    queryFn: () => getProfile(userId).then(r => r.data),
  })

  const { data: recommendations, isLoading: loadingRecs, refetch: refetchRecs } = useQuery({
    queryKey: ['recommendations', userId],
    queryFn: () => getRecommendationsByUser(userId).then(r => r.data),
  })

  const { data: following, refetch: refetchFollowing } = useQuery({
    queryKey: ['following', userId],
    queryFn: () => isFollowing(userId).then(r => r.data),
    enabled: !!user && !isOwn,
  })

  const { data: followers, isLoading: loadingFollowers } = useQuery({
    queryKey: ['followers', userId],
    queryFn: () => getFollowers(userId).then(r => r.data),
  })

  const { data: followingList, isLoading: loadingFollowingList } = useQuery({
    queryKey: ['followingList', userId],
    queryFn: () => getFollowing(userId).then(r => r.data),
    enabled: isOwn,
  })

  async function handleFollow() {
    try {
      if (following) {
        await unfollow(userId)
      } else {
        await follow(userId)
      }
      refetchFollowing()
      queryClient.invalidateQueries({ queryKey: ['feed'] })
      queryClient.invalidateQueries({ queryKey: ['followers', userId] })
      queryClient.invalidateQueries({ queryKey: ['followingList'] })
    } catch (err) {
      console.error(err)
    }
  }

  async function handleSaveBio() {
    setSavingBio(true)
    try {
      await updateProfile({ bio })
      setEditingBio(false)
      refetchProfile()
    } catch (err) {
      console.error(err)
    } finally {
      setSavingBio(false)
    }
  }

  async function handleAvatarChange(e) {
    const file = e.target.files[0]
    if (!file) return
    setUploadingAvatar(true)
    try {
      const { data } = await uploadAvatar(file)
      loginUser({ ...user, avatarUrl: data.avatarUrl }, localStorage.getItem('token'))
      queryClient.invalidateQueries({ queryKey: ['profile', userId] })
      await refetchProfile()
    } catch (err) {
      console.error(err)
    } finally {
      setUploadingAvatar(false)
    }
  }

  if (loadingProfile) {
    return (
      <div className="text-center py-12 text-gray-400 text-sm">Cargando...</div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">

      {/* Cabecera del perfil */}
      <div className="bg-white border border-gray-200 rounded-2xl p-6 mb-6">
        <div className="flex items-start gap-5">

          {/* Avatar */}
          <div className="relative flex-shrink-0">
            {profile?.avatarUrl ? (
              <img
                src={profile.avatarUrl}
                alt={profile.username}
                className="w-20 h-20 rounded-full object-cover border-2 border-gray-100"
              />
            ) : (
              <div className="w-20 h-20 rounded-full bg-indigo-100 flex items-center justify-center text-2xl font-bold text-indigo-600 border-2 border-gray-100">
                {profile?.username.slice(0, 2).toUpperCase()}
              </div>
            )}
            {isOwn && (
              <>
                <button
                  onClick={() => fileRef.current?.click()}
                  disabled={uploadingAvatar}
                  className="absolute -bottom-1 -right-1 w-7 h-7 bg-indigo-600 text-white rounded-full flex items-center justify-center text-xs hover:bg-indigo-700 disabled:opacity-50 shadow"
                >
                  {uploadingAvatar ? '...' : '✎'}
                </button>
                <input
                  ref={fileRef}
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleAvatarChange}
                />
              </>
            )}
          </div>

          {/* Info */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <h1 className="text-xl font-bold text-gray-900 [overflow-wrap:anywhere]">
                  {profile?.username}
                </h1>
                {isOwn && (
                  <p className="text-xs text-gray-400 mt-0.5 [overflow-wrap:anywhere]">
                    {profile?.email}
                  </p>
                )}
              </div>
              {!isOwn && user && (
                <button
                  onClick={handleFollow}
                  className={`text-sm px-4 py-1.5 rounded-lg font-medium transition-colors flex-shrink-0 ${
                    following
                      ? 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                      : 'bg-indigo-600 text-white hover:bg-indigo-700'
                  }`}
                >
                  {following ? 'Siguiendo' : 'Seguir'}
                </button>
              )}
            </div>

            {/* Critic Points */}
            <div className="mt-3 inline-flex items-center gap-1.5 bg-indigo-50 px-3 py-1 rounded-full">
              <span className="text-sm font-semibold text-indigo-700">
                {profile?.criticPoints.toFixed(2)}
              </span>
              <span className="text-xs text-indigo-500">Critic Points</span>
            </div>

            {/* Bio */}
            <div className="mt-3">
              {editingBio ? (
                <div className="space-y-2">
                  <textarea
                    value={bio}
                    onChange={e => setBio(e.target.value)}
                    maxLength={500}
                    rows={3}
                    placeholder="Escribe algo sobre ti..."
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500 resize-none"
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={handleSaveBio}
                      disabled={savingBio}
                      className="text-xs bg-indigo-600 text-white px-3 py-1.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                    >
                      {savingBio ? 'Guardando...' : 'Guardar'}
                    </button>
                    <button
                      onClick={() => setEditingBio(false)}
                      className="text-xs text-gray-500 hover:text-gray-700 px-3 py-1.5"
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              ) : (
                <div className="flex items-start gap-2">
                  <p className="text-sm text-gray-500 flex-1 whitespace-pre-line [overflow-wrap:anywhere]">
                    {profile?.bio || (isOwn ? 'Añade una biografía...' : '')}
                  </p>
                  {isOwn && (
                    <button
                      onClick={() => {
                        setBio(profile?.bio || '')
                        setEditingBio(true)
                      }}
                      className="text-xs text-indigo-500 hover:text-indigo-700 flex-shrink-0"
                    >
                      Editar
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Stats */}
        <div
          className={`mt-5 pt-5 border-t border-gray-100 grid gap-4 text-center ${
            isOwn ? 'grid-cols-4' : 'grid-cols-3'
          }`}
        >
          <div>
            <p className="text-lg font-bold text-gray-900">
              {recommendations?.length ?? 0}
            </p>
            <p className="text-xs text-gray-400">Recomendaciones</p>
          </div>
          <div>
            <p className="text-lg font-bold text-gray-900">
              {recommendations?.filter(r => r.hiddenGem).length ?? 0}
            </p>
            <p className="text-xs text-gray-400">Joyas ocultas</p>
          </div>
          <button
            onClick={() => setOpenList('followers')}
            className="rounded-lg py-1 hover:bg-gray-50 transition-colors"
          >
            <p className="text-lg font-bold text-gray-900">
              {followers?.length ?? 0}
            </p>
            <p className="text-xs text-gray-400">Seguidores</p>
          </button>
          {isOwn && (
            <button
              onClick={() => setOpenList('following')}
              className="rounded-lg py-1 hover:bg-gray-50 transition-colors"
            >
              <p className="text-lg font-bold text-gray-900">
                {followingList?.length ?? 0}
              </p>
              <p className="text-xs text-gray-400">Siguiendo</p>
            </button>
          )}
        </div>
      </div>

      {/* Recomendaciones */}
      <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
        Recomendaciones ({recommendations?.length ?? 0})
      </h2>

      {loadingRecs && (
        <div className="text-center py-8 text-gray-400 text-sm">Cargando...</div>
      )}

      <div className="space-y-4">
        {recommendations?.map(rec => (
          <RecommendationCard key={rec.id} rec={rec} onRated={refetchRecs} />
        ))}
      </div>

      {!loadingRecs && recommendations?.length === 0 && (
        <div className="text-center py-12 text-gray-400 text-sm">
          {isOwn
            ? 'Aún no has publicado ninguna recomendación. ¡Busca contenido y comparte tu primera recomendación!'
            : 'Este usuario aún no ha publicado recomendaciones.'}
        </div>
      )}

      {/* Modales de seguidores / seguidos */}
      {openList === 'followers' && (
        <UserListModal
          title="Seguidores"
          users={followers}
          loading={loadingFollowers}
          onClose={() => setOpenList(null)}
        />
      )}
      {openList === 'following' && isOwn && (
        <UserListModal
          title="Siguiendo"
          users={followingList}
          loading={loadingFollowingList}
          onClose={() => setOpenList(null)}
        />
      )}
    </div>
  )
}
