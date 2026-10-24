package es.udc.fic.fictionary.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Usuario registrado. Puede actuar como curador (publica recomendaciones)
 * y como valorador (puntúa recomendaciones de usuarios a los que sigue).
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 500)
    private String bio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Derivado: suma de las contribuciones de sus recomendaciones (lo mantiene el motor de ranking). */
    @Column(name = "critic_points", nullable = false)
    private double criticPoints;

    protected User() {
        // Requerido por JPA
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getBio() { return bio; }
    public Instant getCreatedAt() { return createdAt; }
    public double getCriticPoints() { return criticPoints; }

    public void setBio(String bio) { this.bio = bio; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setCriticPoints(double criticPoints) { this.criticPoints = criticPoints; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }
}
