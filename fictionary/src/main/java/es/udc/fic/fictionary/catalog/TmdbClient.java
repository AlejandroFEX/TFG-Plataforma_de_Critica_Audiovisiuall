package es.udc.fic.fictionary.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.udc.fic.fictionary.catalog.dto.TmdbGenreDto;
import es.udc.fic.fictionary.catalog.dto.TmdbMovieDto;
import es.udc.fic.fictionary.catalog.dto.TmdbTvDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class TmdbClient {

    private static final String POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";
    /** En modo "joyas": ignora títulos sin votos para no devolver basura sin valorar. */
    private static final int NICHE_MIN_VOTES = 20;

    private final RestClient restClient;
    private final String apiKey;

    public TmdbClient(
            @Value("${fictionary.tmdb.base-url}") String baseUrl,
            @Value("${fictionary.tmdb.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<TmdbMovieDto> searchMovies(String query, int page) {
        try {
            MovieSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("query", query)
                            .queryParam("language", "es-ES")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .body(MovieSearchResponse.class);
            return response != null && response.results() != null
                    ? response.results() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<TmdbTvDto> searchTv(String query, int page) {
        try {
            TvSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/tv")
                            .queryParam("api_key", apiKey)
                            .queryParam("query", query)
                            .queryParam("language", "es-ES")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .body(TvSearchResponse.class);
            return response != null && response.results() != null
                    ? response.results() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Explora por género sin texto de búsqueda. niche = las menos populares primero. */
    public List<TmdbMovieDto> discoverMovies(int genreId, boolean niche) {
        try {
            MovieSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/discover/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", "es-ES")
                            .queryParam("with_genres", genreId)
                            .queryParam("sort_by", niche ? "popularity.asc" : "popularity.desc")
                            .queryParam("vote_count.gte", niche ? NICHE_MIN_VOTES : 0)
                            .queryParam("page", 1)
                            .build())
                    .retrieve()
                    .body(MovieSearchResponse.class);
            return response != null && response.results() != null
                    ? response.results() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<TmdbTvDto> discoverTv(int genreId, boolean niche) {
        try {
            TvSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/discover/tv")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", "es-ES")
                            .queryParam("with_genres", genreId)
                            .queryParam("sort_by", niche ? "popularity.asc" : "popularity.desc")
                            .queryParam("vote_count.gte", niche ? NICHE_MIN_VOTES : 0)
                            .queryParam("page", 1)
                            .build())
                    .retrieve()
                    .body(TvSearchResponse.class);
            return response != null && response.results() != null
                    ? response.results() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Lista completa de géneros de TMDB (con nombre en español). */
    public List<TmdbGenreDto> getGenres(MediaType type) {
        String path = type == MediaType.MOVIE ? "/genre/movie/list" : "/genre/tv/list";
        try {
            GenreListResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("api_key", apiKey)
                            .queryParam("language", "es-ES")
                            .build())
                    .retrieve()
                    .body(GenreListResponse.class);
            return response != null && response.genres() != null
                    ? response.genres() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public TmdbMovieDto getMovie(long tmdbId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/movie/{id}")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", "es-ES")
                            .build(tmdbId))
                    .retrieve()
                    .body(TmdbMovieDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    public TmdbTvDto getTv(long tmdbId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tv/{id}")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", "es-ES")
                            .build(tmdbId))
                    .retrieve()
                    .body(TmdbTvDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String buildPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) return null;
        return POSTER_BASE_URL + posterPath;
    }

    private record MovieSearchResponse(
            @JsonProperty("results") List<TmdbMovieDto> results
    ) {}

    private record TvSearchResponse(
            @JsonProperty("results") List<TmdbTvDto> results
    ) {}

    private record GenreListResponse(
            @JsonProperty("genres") List<TmdbGenreDto> genres
    ) {}
}
