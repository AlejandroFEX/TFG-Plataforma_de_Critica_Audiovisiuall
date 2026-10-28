import client from './client'

export const register = (data) => client.post('/auth/register', data)
export const login = (data) => client.post('/auth/login', data)
export const getMe = () => client.get('/users/me')
export const getProfile = (id) => client.get(`/users/${id}/profile`)
export const getRanking = () => client.get('/ranking')