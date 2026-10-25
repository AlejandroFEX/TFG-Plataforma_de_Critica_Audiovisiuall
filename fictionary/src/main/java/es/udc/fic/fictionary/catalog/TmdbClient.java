package es.udc.fic.fictionary.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.udc.fic.fictionary.catalog.dto.TmdbMovieDto;
import es.udc.fic.fictionary.catalog.dto.TmdbTvDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class TmdbClient {

    private final RestClient restClient;
    private final String apiKey;

    private static final String POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";

    public TmdbClient(
            @Value("${fictionary.tmdb.base-url}") String baseUrl,
            @Value("${fictionary.tmdb.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<TmdbMovieDto> searchMovies(String query) {
        try {
            MovieSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("query", query)
                            .queryParam("language", "es-ES")
                            .queryParam("page", 1)
                            .build())
                    .retrieve()
                    .body(MovieSearchResponse.class);
            return response != null ? response.results() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<TmdbTvDto> searchTv(String query) {
        try {
            TvSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/tv")
                            .queryParam("api_key", apiKey)
                            .queryParam("query", query)
                            .queryParam("language", "es-ES")
                            .queryParam("page", 1)
                            .build())
                    .retrieve()
                    .body(TvSearchResponse.class);
            return response != null ? response.results() : Collections.emptyList();
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

    // Records internos para deserializar la respuesta paginada de TMDB
    private record MovieSearchResponse(
            @JsonProperty("results") List<TmdbMovieDto> results
    ) {}

    private record TvSearchResponse(
            @JsonProperty("results") List<TmdbTvDto> results
    ) {}
}