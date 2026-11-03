import { useState, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getProfile, uploadAvatar, updateProfile } from '../api/auth'
import { getRecommendationsByUser } from '../api/recommendations'
import { follow, unfollow, isFollowing, getFollowers, getFollowing } from '../api/social'
import { useAuth } from '../context/AuthContext'
import RecommendationCard from '../components/RecommendationCard'
import UserListModal from '../components/UserListModal'
import Avatar from '../components/Avatar'

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
  const [openList, setOpenList] = useState(null) // 'followers' | 'following' | null

  const { data: profile, isLoading: loadingProfile, refetch: refetchProfile } = useQuery({
    queryKey: ['profile', userId],
    queryFn: () => getProfile(userId).then(r => r.data),
  })

  const { data: recommendations, isLoading: loadingRecs, refetch: refetchRecs } = useQuery({
    queryKey: ['recommendations', userId],
    queryFn: () => getRecommendationsByUser(userId).then(r => r.data),
  })

  // ¿El usuario autenticado sigue a este perfil? (solo en perfiles ajenos)
  const { data: following, refetch: refetchFollowing } = useQuery({
    queryKey: ['following', userId],
    queryFn: () => isFollowing(userId).then(r => r.data),
    enabled: !!user && !isOwn,
  })

  const { data: followers, isLoading: loadingFollowers } = useQuery({
    queryKey: ['followers', userId],
    queryFn: () => getFollowers(userId).then(r => r.data),
  })

  // La lista de seguidos solo se pide en el perfil propio
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
    return <p className="page py-16 text-center text-sm text-ink-faint">Cargando…</p>
  }

  if (!profile) {
    return (
      <div className="page py-16 text-center">
        <h1 className="mb-1 text-2xl font-semibold">Usuario no encontrado</h1>
        <p className="text-sm text-ink-soft">Puede que el perfil ya no exista.</p>
      </div>
    )
  }

  const gems = recommendations?.filter(r => r.hiddenGem).length ?? 0

  return (
    <div className="page">
      {/* Cabecera */}
      <section className="panel mb-8">
        <div className="flex items-start gap-5 p-6">
          <div className="relative shrink-0">
            <Avatar src={profile.avatarUrl} name={profile.username} size="lg" />
            {isOwn && (
              <>
                <button
                  onClick={() => fileRef.current?.click()}
                  disabled={uploadingAvatar}
                  title="Cambiar foto"
                  className="absolute -bottom-1 -right-1 grid h-7 w-7 place-items-center rounded-full bg-ink text-xs text-white shadow hover:bg-black disabled:opacity-50"
                >
                  {uploadingAvatar ? '…' : '✎'}
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

          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <h1 className="text-3xl font-semibold leading-tight [overflow-wrap:anywhere]">
                  {profile.username}
                </h1>
                {isOwn && (
                  <p className="mt-0.5 text-xs text-ink-faint [overflow-wrap:anywhere]">
                    {profile.email}
                  </p>
                )}
              </div>
              {!isOwn && user && (
                <button
                  onClick={handleFollow}
                  className={`btn shrink-0 ${following ? 'btn-secondary' : 'btn-primary'}`}
                >
                  {following ? 'Siguiendo' : 'Seguir'}
                </button>
              )}
            </div>

            <p className="mt-3 flex items-baseline gap-2">
              <span className="font-serif text-2xl font-semibold tabular-nums text-gem">
                {profile.criticPoints.toFixed(2)}
              </span>
              <span className="label-caps">Critic Points</span>
            </p>

            <div className="mt-3">
              {editingBio ? (
                <div className="space-y-2">
                  <textarea
                    value={bio}
                    onChange={e => setBio(e.target.value)}
                    maxLength={500}
                    rows={3}
                    placeholder="Escribe algo sobre ti…"
                    className="input resize-none"
                  />
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleSaveBio}
                      disabled={savingBio}
                      className="btn btn-primary !px-3 !py-1 text-xs"
                    >
                      {savingBio ? 'Guardando…' : 'Guardar'}
                    </button>
                    <button
                      onClick={() => setEditingBio(false)}
                      className="text-xs text-ink-soft hover:text-ink"
                    >
                      Cancelar
                    </button>
                    <span className="ml-auto text-xs text-ink-faint">{bio.length}/500</span>
                  </div>
                </div>
              ) : (
                <div className="flex items-start gap-3">
                  <p className="flex-1 whitespace-pre-line text-sm text-ink-soft [overflow-wrap:anywhere]">
                    {profile.bio || (isOwn ? 'Añade una biografía…' : '')}
                  </p>
                  {isOwn && (
                    <button
                      onClick={() => {
                        setBio(profile.bio || '')
                        setEditingBio(true)
                      }}
                      className="shrink-0 text-xs text-gem hover:underline"
                    >
                      Editar
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Estadísticas */}
        <div
          className={`grid divide-x divide-line border-t border-line text-center ${
            isOwn ? 'grid-cols-4' : 'grid-cols-3'
          }`}
        >
          <div className="py-3">
            <p className="font-serif text-xl font-semibold tabular-nums">
              {recommendations?.length ?? 0}
            </p>
            <p className="label-caps">Recomendaciones</p>
          </div>
          <div className="py-3">
            <p className="font-serif text-xl font-semibold tabular-nums">{gems}</p>
            <p className="label-caps">Joyas ocultas</p>
          </div>
          <button
            onClick={() => setOpenList('followers')}
            className="py-3 transition-colors hover:bg-paper"
          >
            <p className="font-serif text-xl font-semibold tabular-nums">
              {followers?.length ?? 0}
            </p>
            <p className="label-caps">Seguidores</p>
          </button>
          {isOwn && (
            <button
              onClick={() => setOpenList('following')}
              className="py-3 transition-colors hover:bg-paper"
            >
              <p className="font-serif text-xl font-semibold tabular-nums">
                {followingList?.length ?? 0}
              </p>
              <p className="label-caps">Siguiendo</p>
            </button>
          )}
        </div>
      </section>

      {/* Recomendaciones */}
      <h2 className="label-caps mb-3">
        Recomendaciones · {recommendations?.length ?? 0}
      </h2>

      {loadingRecs && (
        <p className="py-10 text-center text-sm text-ink-faint">Cargando…</p>
      )}

      {recommendations?.length > 0 && (
        <div className="panel divide-y divide-line">
          {recommendations.map(rec => (
            <RecommendationCard key={rec.id} rec={rec} onRated={refetchRecs} />
          ))}
        </div>
      )}

      {!loadingRecs && recommendations?.length === 0 && (
        <p className="py-12 text-center text-sm text-ink-soft">
          {isOwn
            ? 'Aún no has publicado ninguna recomendación. Busca un título y comparte la primera.'
            : 'Este usuario aún no ha publicado recomendaciones.'}
        </p>
      )}

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
