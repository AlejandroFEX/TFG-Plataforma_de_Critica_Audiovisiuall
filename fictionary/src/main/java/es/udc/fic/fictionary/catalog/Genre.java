package es.udc.fic.fictionary.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Género audiovisual según el catálogo de TMDB. */
@Entity
@Table(name = "genre")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_genre_id", nullable = false)
    private int tmdbGenreId;

    @Column(nullable = false, length = 60)
    private String name;

    protected Genre() {
        // Requerido por JPA
    }

    public Genre(int tmdbGenreId, String name) {
        this.tmdbGenreId = tmdbGenreId;
        this.name = name;
    }

    public Long getId() { return id; }
    public int getTmdbGenreId() { return tmdbGenreId; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Genre other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Genre.class.hashCode();
    }
}
