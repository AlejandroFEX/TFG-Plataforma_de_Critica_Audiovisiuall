package es.udc.fic.fictionary.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TitleRepository extends JpaRepository<Title, Long> {

    Optional<Title> findByTmdbIdAndMediaType(long tmdbId, MediaType mediaType);
}
