package es.udc.fic.fictionary.catalog;

import es.udc.fic.fictionary.catalog.dto.TmdbMovieDto;
import es.udc.fic.fictionary.catalog.dto.TmdbTvDto;
import es.udc.fic.fictionary.catalog.dto.TitleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogService {

    private final TitleRepository titleRepository;
    private final GenreRepository genreRepository;
    private final TmdbClient tmdbClient;
    private final double hiddenGemThreshold;

    public CatalogService(
            TitleRepository titleRepository,
            GenreRepository genreRepository,
            TmdbClient tmdbClient,
            @Value("${fictionary.catalog.hidden-gem-threshold:20.0}") double hiddenGemThreshold) {
        this.titleRepository = titleRepository;
        this.genreRepository = genreRepository;
        this.tmdbClient = tmdbClient;
        this.hiddenGemThreshold = hiddenGemThreshold;
    }

    @Transactional(readOnly = true)
    public List<TitleResponse> search(String query, MediaType type) {
        List<TitleResponse> results = new ArrayList<>();

        if (type == null || type == MediaType.MOVIE) {
            tmdbClient.searchMovies(query).forEach(dto ->
                    results.add(movieToResponse(dto)));
        }
        if (type == null || type == MediaType.TV) {
            tmdbClient.searchTv(query).forEach(dto ->
                    results.add(tvToResponse(dto)));
        }

        return results;
    }

    public TitleResponse getOrFetchTitle(long tmdbId, MediaType mediaType) {
        return titleRepository
                .findByTmdbIdAndMediaType(tmdbId, mediaType)
                .map(title -> {
                    // Refrescamos popularidad si tiene más de 24h
                    if (isStale(title)) {
                        refreshFromTmdb(title, mediaType);
                    }
                    return toResponse(title);
                })
                .orElseGet(() -> fetchAndSave(tmdbId, mediaType));
    }

    private TitleResponse fetchAndSave(long tmdbId, MediaType mediaType) {
        if (mediaType == MediaType.MOVIE) {
            TmdbMovieDto dto = tmdbClient.getMovie(tmdbId);
            if (dto == null) throw new IllegalArgumentException(
                    "Título no encontrado en TMDB: " + tmdbId);
            Title title = new Title(dto.id(), MediaType.MOVIE,
                    dto.title(), dto.popularity(), dto.voteCount());
            title.setOverview(dto.overview());
            title.setPosterPath(dto.posterPath());
            title.setReleaseDate(dto.releaseDate());
            titleRepository.save(title);
            return movieToResponse(dto);
        } else {
            TmdbTvDto dto = tmdbClient.getTv(tmdbId);
            if (dto == null) throw new IllegalArgumentException(
                    "Título no encontrado en TMDB: " + tmdbId);
            Title title = new Title(dto.id(), MediaType.TV,
                    dto.name(), dto.popularity(), dto.voteCount());
            title.setOverview(dto.overview());
            title.setPosterPath(dto.posterPath());
            title.setReleaseDate(dto.firstAirDate());
            titleRepository.save(title);
            return tvToResponse(dto);
        }
    }

    private void refreshFromTmdb(Title title, MediaType mediaType) {
        if (mediaType == MediaType.MOVIE) {
            TmdbMovieDto dto = tmdbClient.getMovie(title.getTmdbId());
            if (dto != null) title.refreshPopularity(dto.popularity(), dto.voteCount());
        } else {
            TmdbTvDto dto = tmdbClient.getTv(title.getTmdbId());
            if (dto != null) title.refreshPopularity(dto.popularity(), dto.voteCount());
        }
    }

    private boolean isStale(Title title) {
        return title.getFetchedAt().plusSeconds(86400)
                .isBefore(java.time.Instant.now());
    }

    private TitleResponse movieToResponse(TmdbMovieDto dto) {
        return new TitleResponse(
                null,
                dto.id(),
                MediaType.MOVIE,
                dto.title(),
                dto.overview(),
                tmdbClient.buildPosterUrl(dto.posterPath()),
                dto.releaseDate(),
                dto.popularity(),
                dto.voteCount(),
                dto.popularity() < hiddenGemThreshold,
                Set.of()
        );
    }

    private TitleResponse tvToResponse(TmdbTvDto dto) {
        return new TitleResponse(
                null,
                dto.id(),
                MediaType.TV,
                dto.name(),
                dto.overview(),
                tmdbClient.buildPosterUrl(dto.posterPath()),
                dto.firstAirDate(),
                dto.popularity(),
                dto.voteCount(),
                dto.popularity() < hiddenGemThreshold,
                Set.of()
        );
    }

    private TitleResponse toResponse(Title title) {
        Set<String> genres = title.getGenres().stream()
                .map(Genre::getName)
                .collect(Collectors.toSet());
        return new TitleResponse(
                title.getId(),
                title.getTmdbId(),
                title.getMediaType(),
                title.getName(),
                title.getOverview(),
                tmdbClient.buildPosterUrl(title.getPosterPath()),
                title.getReleaseDate(),
                title.getPopularity(),
                title.getVoteCount(),
                title.getPopularity() < hiddenGemThreshold,
                genres
        );
    }
}