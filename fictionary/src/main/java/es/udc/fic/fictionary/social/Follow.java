package es.udc.fic.fictionary.social;

import es.udc.fic.fictionary.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** Relación de seguimiento: {@code follower} sigue a {@code followed}. */
@Entity
@Table(name = "follow")
public class Follow {

    @EmbeddedId
    private FollowId id;

    @MapsId("followerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id")
    private User follower;

    @MapsId("followedId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followed_id")
    private User followed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Follow() {
        // Requerido por JPA
    }

    public Follow(User follower, User followed) {
        if (follower == null || followed == null) {
            throw new IllegalArgumentException("Seguidor y seguido son obligatorios");
        }
        if (follower.equals(followed)) {
            throw new IllegalArgumentException("Un usuario no puede seguirse a sí mismo");
        }
        this.follower = follower;
        this.followed = followed;
        this.id = new FollowId(follower.getId(), followed.getId());
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public FollowId getId() { return id; }
    public User getFollower() { return follower; }
    public User getFollowed() { return followed; }
    public Instant getCreatedAt() { return createdAt; }
}
