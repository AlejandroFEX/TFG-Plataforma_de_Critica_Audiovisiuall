package es.udc.fic.fictionary.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Copia local de los metadatos de una película o serie obtenidos de TMDB (caché).
 * No almacena el contenido audiovisual, solo metadatos.
 */
@Entity
@Table(name = "title")
public class Title {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private long tmdbId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(columnDefinition = "text")
    private String overview;

    @Column(name = "poster_path", length = 255)
    private String posterPath;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    /** Popularidad actual en TMDB (puede variar a diario). */
    @Column(nullable = false)
    private double popularity;

    /** Votos acumulados en TMDB: medida alternativa de popularidad. */
    @Column(name = "vote_count", nullable = false)
    private int voteCount;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @ManyToMany
    @JoinTable(name = "title_genre",
            joinColumns = @JoinColumn(name = "title_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    protected Title() {
        // Requerido por JPA
    }

    public Title(long tmdbId, MediaType mediaType, String name, double popularity, int voteCount) {
        if (popularity < 0) {
            throw new IllegalArgumentException("La popularidad no puede ser negativa");
        }
        if (voteCount < 0) {
            throw new IllegalArgumentException("El número de votos no puede ser negativo");
        }
        this.tmdbId = tmdbId;
        this.mediaType = mediaType;
        this.name = name;
        this.popularity = popularity;
        this.voteCount = voteCount;
    }

    @PrePersist
    void onCreate() {
        if (fetchedAt == null) {
            fetchedAt = Instant.now();
        }
    }

    /** Actualiza los datos que cambian con el tiempo tras una nueva consulta a TMDB. */
    public void refreshPopularity(double popularity, int voteCount) {
        if (popularity < 0 || voteCount < 0) {
            throw new IllegalArgumentException("Popularidad y votos no pueden ser negativos");
        }
        this.popularity = popularity;
        this.voteCount = voteCount;
        this.fetchedAt = Instant.now();
    }

    public Long getId() { return id; }
    public long getTmdbId() { return tmdbId; }
    public MediaType getMediaType() { return mediaType; }
    public String getName() { return name; }
    public String getOverview() { return overview; }
    public String getPosterPath() { return posterPath; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public double getPopularity() { return popularity; }
    public int getVoteCount() { return voteCount; }
    public Instant getFetchedAt() { return fetchedAt; }
    public Set<Genre> getGenres() { return genres; }

    public void setOverview(String overview) { this.overview = overview; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Title other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Title.class.hashCode();
    }
}
