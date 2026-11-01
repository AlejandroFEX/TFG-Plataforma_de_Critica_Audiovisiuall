package es.udc.fic.fictionary.catalog;

import es.udc.fic.fictionary.catalog.dto.GenreResponse;
import es.udc.fic.fictionary.catalog.dto.TitleResponse;
import es.udc.fic.fictionary.catalog.dto.TmdbGenreDto;
import es.udc.fic.fictionary.catalog.dto.TmdbMovieDto;
import es.udc.fic.fictionary.catalog.dto.TmdbTvDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    /** Géneros conocidos (los que ya han aparecido en algún título guardado). */
    @Transactional(readOnly = true)
    public List<GenreResponse> getGenres() {
        return genreRepository.findAllByOrderByNameAsc().stream()
                .map(g -> new GenreResponse(g.getTmdbGenreId(), g.getName()))
                .toList();
    }

    public TitleResponse getOrFetchTitle(long tmdbId, MediaType mediaType) {
        return titleRepository
                .findByTmdbIdAndMediaType(tmdbId, mediaType)
                .map(title -> {
                    // Refresca si tiene más de 24 h o si aún no tiene géneros guardados
                    if (isStale(title) || title.getGenres().isEmpty()) {
                        refreshFromTmdb(title, mediaType);
                    }
                    return toResponse(title);
                })
                .orElseGet(() -> fetchAndSave(tmdbId, mediaType));
    }

    private TitleResponse fetchAndSave(long tmdbId, MediaType mediaType) {
        Title title;
        if (mediaType == MediaType.MOVIE) {
            TmdbMovieDto dto = tmdbClient.getMovie(tmdbId);
            if (dto == null) throw new IllegalArgumentException(
                    "Título no encontrado en TMDB: " + tmdbId);
            title = new Title(dto.id(), MediaType.MOVIE,
                    dto.title(), dto.popularity(), dto.voteCount());
            title.setOverview(dto.overview());
            title.setPosterPath(dto.posterPath());
            title.setReleaseDate(dto.releaseDate());
            applyGenres(title, dto.genres());
        } else {
            TmdbTvDto dto = tmdbClient.getTv(tmdbId);
            if (dto == null) throw new IllegalArgumentException(
                    "Título no encontrado en TMDB: " + tmdbId);
            title = new Title(dto.id(), MediaType.TV,
                    dto.name(), dto.popularity(), dto.voteCount());
            title.setOverview(dto.overview());
            title.setPosterPath(dto.posterPath());
            title.setReleaseDate(dto.firstAirDate());
            applyGenres(title, dto.genres());
        }
        titleRepository.save(title);
        return toResponse(title);
    }

    private void refreshFromTmdb(Title title, MediaType mediaType) {
        if (mediaType == MediaType.MOVIE) {
            TmdbMovieDto dto = tmdbClient.getMovie(title.getTmdbId());
            if (dto != null) {
                title.refreshPopularity(dto.popularity(), dto.voteCount());
                applyGenres(title, dto.genres());
            }
        } else {
            TmdbTvDto dto = tmdbClient.getTv(title.getTmdbId());
            if (dto != null) {
                title.refreshPopularity(dto.popularity(), dto.voteCount());
                applyGenres(title, dto.genres());
            }
        }
    }

    /** Enlaza los géneros de TMDB con el título, creándolos si es la primera vez que se ven. */
    private void applyGenres(Title title, List<TmdbGenreDto> dtos) {
        if (dtos == null) return;
        for (TmdbGenreDto g : dtos) {
            Genre genre = genreRepository.findByTmdbGenreId(g.id())
                    .orElseGet(() -> genreRepository.save(new Genre(g.id(), g.name())));
            title.getGenres().add(genre);
        }
    }

    private boolean isStale(Title title) {
        return title.getFetchedAt().plusSeconds(86400).isBefore(Instant.now());
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
