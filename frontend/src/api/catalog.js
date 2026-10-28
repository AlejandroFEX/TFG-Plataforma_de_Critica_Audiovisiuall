import client from './client'

export const searchCatalog = (query, type) =>
  client.get('/catalog/search', { params: { query, type } })

export const getTitle = (tmdbId, type) =>
  client.get(`/catalog/${tmdbId}`, { params: { type } })