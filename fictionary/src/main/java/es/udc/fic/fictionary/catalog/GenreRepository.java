package es.udc.fic.fictionary.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByTmdbGenreId(int tmdbGenreId);

    List<Genre> findAllByOrderByNameAsc();
}
