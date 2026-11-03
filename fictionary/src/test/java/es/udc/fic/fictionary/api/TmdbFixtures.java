package es.udc.fic.fictionary.api;

import es.udc.fic.fictionary.catalog.dto.TmdbGenreDto;
import es.udc.fic.fictionary.catalog.dto.TmdbMovieDto;
import es.udc.fic.fictionary.catalog.dto.TmdbTvDto;

import java.time.LocalDate;
import java.util.List;

/** Respuestas de TMDB de mentira para las pruebas (no se llama nunca a la API real). */
final class TmdbFixtures {

    static final TmdbGenreDto SCI_FI = new TmdbGenreDto(878, "Ciencia ficcion");
    static final TmdbGenreDto DRAMA = new TmdbGenreDto(18, "Drama");
    static final TmdbGenreDto HORROR = new TmdbGenreDto(27, "Terror");

    private TmdbFixtures() {
    }

    static TmdbMovieDto movie(long id, String title, double popularity,
                              List<Integer> genreIds, List<TmdbGenreDto> genres) {
        return new TmdbMovieDto(id, title, "Sinopsis de " + title, "/p" + id + ".jpg",
                LocalDate.of(2010, 7, 15), popularity, 100, genreIds, genres);
    }

    static TmdbTvDto tv(long id, String name, double popularity,
                        List<Integer> genreIds, List<TmdbGenreDto> genres) {
        return new TmdbTvDto(id, name, "Sinopsis de " + name, "/p" + id + ".jpg",
                LocalDate.of(2015, 3, 1), popularity, 100, genreIds, genres);
    }
}
