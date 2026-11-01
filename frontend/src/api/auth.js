import client from './client'

export const register = (data) => client.post('/auth/register', data)
export const login = (data) => client.post('/auth/login', data)
export const getMe = () => client.get('/users/me')
export const getProfile = (id) => client.get(`/users/${id}/profile`)
export const getRanking = () => client.get('/ranking')
export const searchUsers = (q) => client.get('/users/search', { params: { q } })
export const updateProfile = (data) => client.patch('/users/me', data)
export const uploadAvatar = (file) => {
  const form = new FormData()
  form.append('file', file)
  return client.post('/users/me/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
