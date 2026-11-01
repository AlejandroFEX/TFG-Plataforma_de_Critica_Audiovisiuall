package es.udc.fic.fictionary.social;

import es.udc.fic.fictionary.recommendation.Recommendation;
import es.udc.fic.fictionary.recommendation.RecommendationRepository;
import es.udc.fic.fictionary.recommendation.dto.RecommendationResponse;
import es.udc.fic.fictionary.social.dto.UserSummaryResponse;
import es.udc.fic.fictionary.user.User;
import es.udc.fic.fictionary.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
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
    public List<UserSummaryResponse> getFollowers(Long userId) {
        return followRepository.findFollowers(userId).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getFollowing(Long userId) {
        return followRepository.findFollowing(userId).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getFeed(Long userId, FeedFilter f) {
        List<Long> followedIds = followRepository.findFollowedIds(userId);
        if (followedIds.isEmpty()) {
            return List.of();
        }

        Instant fromInstant = f.from() == null ? null
                : f.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = f.to() == null ? null
                : f.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return recommendationRepository.findFeedByCuratorIds(followedIds).stream()
                .filter(r -> f.mediaType() == null
                        || r.getTitle().getMediaType() == f.mediaType())
                .filter(r -> f.genre() == null
                        || r.getTitle().getGenres().stream()
                                .anyMatch(g -> g.getTmdbGenreId() == f.genre()))
                .filter(r -> f.minPopularity() == null
                        || r.getPopularitySnapshot() >= f.minPopularity())
                .filter(r -> f.maxPopularity() == null
                        || r.getPopularitySnapshot() < f.maxPopularity())
                .filter(r -> f.minRating() == null
                        || (r.getRatingCount() > 0 && r.averageRating() >= f.minRating()))
                .filter(r -> fromInstant == null || !r.getCreatedAt().isBefore(fromInstant))
                .filter(r -> toInstant == null || r.getCreatedAt().isBefore(toInstant))
                .sorted(comparatorFor(f.sortBy()))
                .map(this::toResponse)
                .toList();
    }

    private Comparator<Recommendation> comparatorFor(String sortBy) {
        String key = sortBy == null ? "recent" : sortBy;
        return switch (key) {
            case "oldest" -> Comparator.comparing(Recommendation::getCreatedAt);
            case "criticPoints" -> Comparator
                    .comparingDouble((Recommendation r) -> r.getCurator().getCriticPoints())
                    .thenComparing(Recommendation::getCreatedAt)
                    .reversed();
            case "rating" -> Comparator
                    .comparingDouble(Recommendation::averageRating)
                    .thenComparingInt(Recommendation::getRatingCount)
                    .reversed();
            case "leastPopular" -> Comparator
                    .comparingDouble(Recommendation::getPopularitySnapshot)
                    .thenComparing(Recommendation::getCreatedAt, Comparator.reverseOrder());
            default -> Comparator.comparing(Recommendation::getCreatedAt).reversed();
        };
    }

    private UserSummaryResponse toSummary(User u) {
        return new UserSummaryResponse(
                u.getId(), u.getUsername(), u.getAvatarUrl(), u.getCriticPoints());
    }

    private RecommendationResponse toResponse(Recommendation r) {
        String posterUrl = r.getTitle().getPosterPath() != null
                ? "https://image.tmdb.org/t/p/w500" + r.getTitle().getPosterPath()
                : null;
        return new RecommendationResponse(
                r.getId(),
                r.getCurator().getId(),
                r.getCurator().getUsername(),
                r.getCurator().getAvatarUrl(),
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
