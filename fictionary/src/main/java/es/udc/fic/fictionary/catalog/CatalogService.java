package es.udc.fic.fictionary.catalog;

import es.udc.fic.fictionary.common.NotFoundException;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogService {

    /** Páginas de TMDB que se revisan cuando se combina texto + género (20 resultados/página). */
    private static final int FILTER_PAGES = 3;

    private final TitleRepository titleRepository;
    private final GenreRepository genreRepository;
    private final TmdbClient tmdbClient;
    private final double hiddenGemThreshold;

    /** Caché en memoria de id -> nombre de género por tipo (casi nunca cambian). */
    private final Map<MediaType, Map<Integer, String>> genreCache = new ConcurrentHashMap<>();

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

    // ------------------------------------------------------------------
    // Búsqueda y géneros
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TitleResponse> search(String query, MediaType type, Integer genre, boolean niche) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty() && genre == null) {
            return List.of();
        }
        List<TitleResponse> results = new ArrayList<>();
        if (type == null || type == MediaType.MOVIE) {
            findMovies(q, genre, niche).forEach(dto -> results.add(movieToResponse(dto)));
        }
        if (type == null || type == MediaType.TV) {
            findTv(q, genre, niche).forEach(dto -> results.add(tvToResponse(dto)));
        }
        return results;
    }

    private List<TmdbMovieDto> findMovies(String query, Integer genre, boolean niche) {
        if (query.isEmpty()) {
            return tmdbClient.discoverMovies(genre, niche);
        }
        List<TmdbMovieDto> found = new ArrayList<>();
        int pages = genre == null ? 1 : FILTER_PAGES;
        for (int page = 1; page <= pages; page++) {
            List<TmdbMovieDto> batch = tmdbClient.searchMovies(query, page);
            if (batch.isEmpty()) break;
            found.addAll(batch);
        }
        if (genre == null) return found;
        return found.stream()
                .filter(m -> m.genreIds() != null && m.genreIds().contains(genre))
                .toList();
    }

    private List<TmdbTvDto> findTv(String query, Integer genre, boolean niche) {
        if (query.isEmpty()) {
            return tmdbClient.discoverTv(genre, niche);
        }
        List<TmdbTvDto> found = new ArrayList<>();
        int pages = genre == null ? 1 : FILTER_PAGES;
        for (int page = 1; page <= pages; page++) {
            List<TmdbTvDto> batch = tmdbClient.searchTv(query, page);
            if (batch.isEmpty()) break;
            found.addAll(batch);
        }
        if (genre == null) return found;
        return found.stream()
                .filter(t -> t.genreIds() != null && t.genreIds().contains(genre))
                .toList();
    }

    /** Géneros de TMDB. Sin tipo: películas y series fusionadas. Si TMDB falla, los de la BD. */
    @Transactional(readOnly = true)
    public List<GenreResponse> getGenres(MediaType type) {
        Map<Integer, String> merged = new HashMap<>();
        if (type == null || type == MediaType.MOVIE) {
            merged.putAll(genreNames(MediaType.MOVIE));
        }
        if (type == null || type == MediaType.TV) {
            merged.putAll(genreNames(MediaType.TV));
        }
        if (merged.isEmpty()) {
            genreRepository.findAllByOrderByNameAsc()
                    .forEach(g -> merged.put(g.getTmdbGenreId(), g.getName()));
        }
        return merged.entrySet().stream()
                .map(e -> new GenreResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(GenreResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<Integer, String> genreNames(MediaType type) {
        Map<Integer, String> cached = genreCache.get(type);
        if (cached != null) return cached;
        Map<Integer, String> fresh = tmdbClient.getGenres(type).stream()
                .collect(Collectors.toMap(TmdbGenreDto::id, TmdbGenreDto::name, (a, b) -> a));
        if (!fresh.isEmpty()) {
            genreCache.put(type, fresh);   // no se cachea un fallo de TMDB
        }
        return fresh;
    }

    private Set<String> namesFor(List<Integer> ids, MediaType type) {
        if (ids == null || ids.isEmpty()) return Set.of();
        Map<Integer, String> names = genreNames(type);
        return ids.stream()
                .map(names::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // ------------------------------------------------------------------
    // Detalle de un título
    // ------------------------------------------------------------------

    public TitleResponse getOrFetchTitle(long tmdbId, MediaType mediaType) {
        return titleRepository
                .findByTmdbIdAndMediaType(tmdbId, mediaType)
                .map(title -> {
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
            if (dto == null) throw new NotFoundException("Título no encontrado en TMDB: " + tmdbId);
            title = new Title(dto.id(), MediaType.MOVIE,
                    dto.title(), dto.popularity(), dto.voteCount());
            title.setOverview(dto.overview());
            title.setPosterPath(dto.posterPath());
            title.setReleaseDate(dto.releaseDate());
            applyGenres(title, dto.genres());
        } else {
            TmdbTvDto dto = tmdbClient.getTv(tmdbId);
            if (dto == null) throw new NotFoundException("Título no encontrado en TMDB: " + tmdbId);
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

    // ------------------------------------------------------------------
    // Conversión a respuesta
    // ------------------------------------------------------------------

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
                namesFor(dto.genreIds(), MediaType.MOVIE)
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
                namesFor(dto.genreIds(), MediaType.TV)
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
