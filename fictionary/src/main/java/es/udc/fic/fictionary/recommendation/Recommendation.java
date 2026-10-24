package es.udc.fic.fictionary.recommendation;

import es.udc.fic.fictionary.catalog.Title;
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
 * Recomendación de un curador sobre un título. Es la unidad sobre la que se valora y
 * sobre la que se calculan los Critic Points.
 *
 * <p>Conserva una instantánea inmutable de la popularidad del título al publicarse (RF-REC-04)
 * y los agregados derivados {@code n_r}, {@code S_r} y {@code x_r} del modelo de ranking.
 */
@Entity
@Table(name = "recommendation")
public class Recommendation {

    public static final int MAX_TEXT_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curator_id", nullable = false, updatable = false)
    private User curator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false, updatable = false)
    private Title title;

    @Column(name = "body", nullable = false, length = MAX_TEXT_LENGTH)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // --- Instantánea de popularidad: inmutable (IN-08) ---

    @Column(name = "popularity_snapshot", nullable = false, updatable = false)
    private double popularitySnapshot;

    @Column(name = "vote_count_snapshot", nullable = false, updatable = false)
    private int voteCountSnapshot;

    /** Verdadero si la popularidad no estaba disponible y se imputó (p. ej. con la mediana del catálogo). */
    @Column(name = "popularity_imputed", nullable = false, updatable = false)
    private boolean popularityImputed;

    // --- Agregados derivados que mantiene el motor de ranking (IN-09) ---

    /** n_r: número de valoraciones recibidas. */
    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    /** S_r: suma de las puntuaciones recibidas. */
    @Column(name = "rating_sum", nullable = false)
    private long ratingSum;

    /** x_r: puntos que esta recomendación aporta a su curador. */
    @Column(nullable = false)
    private double contribution;

    protected Recommendation() {
        // Requerido por JPA
    }

    /** Crea una recomendación tomando la instantánea de popularidad del propio título. */
    public Recommendation(User curator, Title title, String text) {
        this(curator, title, text, title.getPopularity(), title.getVoteCount(), false);
    }

    public Recommendation(User curator, Title title, String text,
                          double popularitySnapshot, int voteCountSnapshot, boolean popularityImputed) {
        if (curator == null || title == null) {
            throw new IllegalArgumentException("Curador y título son obligatorios");
        }
        if (popularitySnapshot < 0 || voteCountSnapshot < 0) {
            throw new IllegalArgumentException("La instantánea de popularidad no puede ser negativa");
        }
        this.curator = curator;
        this.title = title;
        this.text = validateText(text);
        this.popularitySnapshot = popularitySnapshot;
        this.voteCountSnapshot = voteCountSnapshot;
        this.popularityImputed = popularityImputed;
    }

    private static String validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("El texto de la recomendación es obligatorio");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "El texto no puede superar los " + MAX_TEXT_LENGTH + " caracteres");
        }
        return text;
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

    // ---------------------------------------------------------------------
    // Operaciones sobre los agregados (la fórmula de x_r vive en el paquete ranking)
    // ---------------------------------------------------------------------

    /** Alta de una valoración: n_r += 1, S_r += score. */
    public void addRating(int score) {
        Rating.requireValidScore(score);
        ratingCount += 1;
        ratingSum += score;
    }

    /** Modificación de una valoración: S_r += nueva - anterior. */
    public void changeRating(int oldScore, int newScore) {
        Rating.requireValidScore(oldScore);
        Rating.requireValidScore(newScore);
        if (ratingCount == 0) {
            throw new IllegalStateException("No hay valoraciones que modificar");
        }
        ratingSum += (long) newScore - oldScore;
    }

    /** Retirada de una valoración: n_r -= 1, S_r -= score. */
    public void removeRating(int score) {
        Rating.requireValidScore(score);
        if (ratingCount == 0) {
            throw new IllegalStateException("No hay valoraciones que retirar");
        }
        ratingCount -= 1;
        ratingSum -= score;
    }

    public void editText(String newText) {
        this.text = validateText(newText);
    }

    public void setContribution(double contribution) {
        this.contribution = contribution;
    }

    public Long getId() { return id; }
    public User getCurator() { return curator; }
    public Title getTitle() { return title; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public double getPopularitySnapshot() { return popularitySnapshot; }
    public int getVoteCountSnapshot() { return voteCountSnapshot; }
    public boolean isPopularityImputed() { return popularityImputed; }
    public int getRatingCount() { return ratingCount; }
    public long getRatingSum() { return ratingSum; }
    public double getContribution() { return contribution; }

    /** Puntuación media, o 0 si aún no tiene valoraciones. */
    public double averageRating() {
        return ratingCount == 0 ? 0.0 : (double) ratingSum / ratingCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recommendation other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Recommendation.class.hashCode();
    }
}
