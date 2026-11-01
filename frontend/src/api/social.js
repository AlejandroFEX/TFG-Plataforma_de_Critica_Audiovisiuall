import client from './client'

export const follow = (userId) => client.post(`/users/${userId}/follow`)
export const unfollow = (userId) => client.delete(`/users/${userId}/follow`)
export const isFollowing = (userId) => client.get(`/users/${userId}/follow`)
export const getFeed = (params) => client.get('/feed', { params })
export const getFollowers = (userId) => client.get(`/users/${userId}/followers`)
export const getFollowing = (userId) => client.get(`/users/${userId}/following`)