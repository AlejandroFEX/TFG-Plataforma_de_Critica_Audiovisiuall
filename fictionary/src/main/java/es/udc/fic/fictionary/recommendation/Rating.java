package es.udc.fic.fictionary.recommendation;

import es.udc.fic.fictionary.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Valoración (1 a 10) de la <em>utilidad de una recomendación</em>, no de la obra (RF-VAL-06).
 */
@Entity
@Table(name = "rating")
public class Rating {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rater_id", nullable = false, updatable = false)
    private User rater;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false, updatable = false)
    private Recommendation recommendation;

    @Column(nullable = false)
    private int score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Rating() {
        // Requerido por JPA
    }

    public Rating(User rater, Recommendation recommendation, int score) {
        if (rater == null || recommendation == null) {
            throw new IllegalArgumentException("Valorador y recomendación son obligatorios");
        }
        if (rater.equals(recommendation.getCurator())) {
            throw new IllegalArgumentException("Un usuario no puede valorar sus propias recomendaciones");
        }
        requireValidScore(score);
        this.rater = rater;
        this.recommendation = recommendation;
        this.score = score;
    }

    static void requireValidScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException(
                    "La puntuación debe estar entre " + MIN_SCORE + " y " + MAX_SCORE);
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Cambia la puntuación y devuelve la anterior (la necesita el recálculo incremental). */
    public int changeScore(int newScore) {
        requireValidScore(newScore);
        int previous = this.score;
        this.score = newScore;
        return previous;
    }

    public Long getId() { return id; }
    public User getRater() { return rater; }
    public Recommendation getRecommendation() { return recommendation; }
    public int getScore() { return score; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rating other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Rating.class.hashCode();
    }
}
