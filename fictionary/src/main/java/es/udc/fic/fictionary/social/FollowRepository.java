package es.udc.fic.fictionary.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import es.udc.fic.fictionary.user.User;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    @Query("select count(f) > 0 from Follow f where f.id.followerId = :followerId and f.id.followedId = :followedId")
    boolean isFollowing(@Param("followerId") Long followerId, @Param("followedId") Long followedId);

    @Query("select count(f) from Follow f where f.id.followedId = :userId")
    long countFollowers(@Param("userId") Long userId);

    @Query("select count(f) from Follow f where f.id.followerId = :userId")
    long countFollowing(@Param("userId") Long userId);

    @Query("select f.id.followedId from Follow f where f.id.followerId = :followerId")
    List<Long> findFollowedIds(@Param("followerId") Long followerId);

    @Query("select f.follower from Follow f where f.id.followedId = :userId order by f.follower.username")
    List<User> findFollowers(@Param("userId") Long userId);

    @Query("select f.followed from Follow f where f.id.followerId = :userId order by f.followed.username")
    List<User> findFollowing(@Param("userId") Long userId);
}

