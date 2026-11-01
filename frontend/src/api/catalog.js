import client from './client'

export const searchCatalog = ({ query, type, genre, niche }) =>
  client.get('/catalog/search', {
    params: {
      query: query || '',
      type,
      genre: genre || undefined,
      niche: niche || undefined,
    },
  })

export const getTitle = (tmdbId, type) =>
  client.get(`/catalog/${tmdbId}`, { params: { type } })

export const getGenres = (type) =>
  client.get('/catalog/genres', { params: { type } })
