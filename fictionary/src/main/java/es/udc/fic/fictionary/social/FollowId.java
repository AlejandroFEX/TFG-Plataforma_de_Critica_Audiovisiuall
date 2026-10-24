package es.udc.fic.fictionary.social;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Clave primaria compuesta de {@link Follow}. */
@Embeddable
public class FollowId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "follower_id")
    private Long followerId;

    @Column(name = "followed_id")
    private Long followedId;

    protected FollowId() {
        // Requerido por JPA
    }

    public FollowId(Long followerId, Long followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
    }

    public Long getFollowerId() { return followerId; }
    public Long getFollowedId() { return followedId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowId other)) return false;
        return Objects.equals(followerId, other.followerId) && Objects.equals(followedId, other.followedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followedId);
    }
}
