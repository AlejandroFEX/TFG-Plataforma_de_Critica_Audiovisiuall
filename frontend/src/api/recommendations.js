import client from './client'

export const publishRecommendation = (data) =>
  client.post('/recommendations', data)

export const rateRecommendation = (id, score) =>
  client.post(`/recommendations/${id}/ratings`, { score })

export const getRecommendationsByUser = (userId) =>
  client.get(`/recommendations/users/${userId}`)

export const getRecommendationById = (id) =>
  client.get(`/recommendations/${id}`)

export const deleteRecommendation = (id) =>
  client.delete(`/recommendations/${id}`)