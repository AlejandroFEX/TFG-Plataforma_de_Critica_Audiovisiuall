package es.udc.fic.fictionary.social;

import es.udc.fic.fictionary.catalog.MediaType;
import es.udc.fic.fictionary.recommendation.RecommendationRepository;
import es.udc.fic.fictionary.recommendation.dto.RecommendationResponse;
import es.udc.fic.fictionary.user.User;
import es.udc.fic.fictionary.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SocialService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final RecommendationRepository recommendationRepository;
    private final double hiddenGemThreshold;

    public SocialService(
            FollowRepository followRepository,
            UserRepository userRepository,
            RecommendationRepository recommendationRepository,
            @Value("${fictionary.catalog.hidden-gem-threshold:20.0}") double hiddenGemThreshold) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.recommendationRepository = recommendationRepository;
        this.hiddenGemThreshold = hiddenGemThreshold;
    }

    public void follow(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("No puedes seguirte a ti mismo");
        }
        if (followRepository.isFollowing(followerId, followedId)) {
            throw new IllegalArgumentException("Ya sigues a este usuario");
        }
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + followerId));
        User followed = userRepository.findById(followedId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + followedId));
        followRepository.save(new Follow(follower, followed));
    }

    public void unfollow(Long followerId, Long followedId) {
        FollowId id = new FollowId(followerId, followedId);
        if (!followRepository.existsById(id)) {
            throw new IllegalArgumentException("No sigues a este usuario");
        }
        followRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followedId) {
        return followRepository.isFollowing(followerId, followedId);
    }

    @Transactional(readOnly = true)
    public long countFollowers(Long userId) {
        return followRepository.countFollowers(userId);
    }

    @Transactional(readOnly = true)
    public long countFollowing(Long userId) {
        return followRepository.countFollowing(userId);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getFeed(Long userId,
                                                MediaType mediaType,
                                                Double maxPopularity,
                                                String sortBy) {
        List<Long> followedIds = followRepository.findFollowedIds(userId);
        if (followedIds.isEmpty()) {
            return List.of();
        }

        if (mediaType != null || maxPopularity != null) {
            return recommendationRepository
                    .findFeedFiltered(followedIds, mediaType, maxPopularity)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return recommendationRepository
                .findFeedByCuratorIds(followedIds)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RecommendationResponse toResponse(
            es.udc.fic.fictionary.recommendation.Recommendation r) {
        String posterUrl = r.getTitle().getPosterPath() != null
                ? "https://image.tmdb.org/t/p/w500" + r.getTitle().getPosterPath()
                : null;
        return new RecommendationResponse(
                r.getId(),
                r.getCurator().getId(),
                r.getCurator().getUsername(),
                r.getCurator().getCriticPoints(),
                r.getTitle().getId(),
                r.getTitle().getTmdbId(),
                r.getTitle().getMediaType(),
                r.getTitle().getName(),
                posterUrl,
                r.getPopularitySnapshot(),
                r.getPopularitySnapshot() < hiddenGemThreshold,
                r.getText(),
                r.getRatingCount(),
                r.averageRating(),
                r.getContribution(),
                r.getCreatedAt()
        );
    }
}