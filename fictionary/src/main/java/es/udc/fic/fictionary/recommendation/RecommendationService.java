package es.udc.fic.fictionary.recommendation;

import es.udc.fic.fictionary.catalog.CatalogService;
import es.udc.fic.fictionary.catalog.MediaType;
import es.udc.fic.fictionary.catalog.Title;
import es.udc.fic.fictionary.catalog.TitleRepository;
import es.udc.fic.fictionary.ranking.ContributionCalculator;
import es.udc.fic.fictionary.ranking.PopularityInfo;
import es.udc.fic.fictionary.recommendation.dto.RecommendationResponse;
import es.udc.fic.fictionary.user.User;
import es.udc.fic.fictionary.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final TitleRepository titleRepository;
    private final CatalogService catalogService;
    private final ContributionCalculator contributionCalculator;
    private final double hiddenGemThreshold;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            RatingRepository ratingRepository,
            UserRepository userRepository,
            TitleRepository titleRepository,
            CatalogService catalogService,
            ContributionCalculator contributionCalculator,
            @Value("${fictionary.catalog.hidden-gem-threshold:20.0}") double hiddenGemThreshold) {
        this.recommendationRepository = recommendationRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.titleRepository = titleRepository;
        this.catalogService = catalogService;
        this.contributionCalculator = contributionCalculator;
        this.hiddenGemThreshold = hiddenGemThreshold;
    }

    public RecommendationResponse publish(Long curatorId, long tmdbId,
                                          MediaType mediaType, String text) {
        User curator = userRepository.findById(curatorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + curatorId));

        catalogService.getOrFetchTitle(tmdbId, mediaType);
        Title title = titleRepository.findByTmdbIdAndMediaType(tmdbId, mediaType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Título no encontrado: " + tmdbId));

        if (recommendationRepository.existsByCuratorIdAndTitleId(curatorId, title.getId())) {
            throw new IllegalArgumentException(
                    "Ya has publicado una recomendación para este título");
        }

        Recommendation recommendation = new Recommendation(curator, title, text);
        recommendationRepository.save(recommendation);
        return toResponse(recommendation);
    }

    public RecommendationResponse rate(Long raterId, Long recommendationId, int score) {
        User rater = userRepository.findById(raterId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + raterId));

        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recomendación no encontrada: " + recommendationId));

        ratingRepository.findByRaterIdAndRecommendationId(raterId, recommendationId)
                .ifPresentOrElse(
                        existing -> {
                            int oldScore = existing.changeScore(score);
                            recommendation.changeRating(oldScore, score);
                        },
                        () -> {
                            Rating rating = new Rating(rater, recommendation, score);
                            ratingRepository.save(rating);
                            recommendation.addRating(score);
                        }
                );

        double contribution = contributionCalculator.contribution(
                recommendation.getRatingCount(),
                recommendation.getRatingSum(),
                PopularityInfo.of(recommendation.getPopularitySnapshot()));
        recommendation.setContribution(contribution);
        recalculateCriticPoints(recommendation.getCurator());

        return toResponse(recommendation);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getByUser(Long userId) {
        return recommendationRepository
                .findByCuratorIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getById(Long id) {
        return recommendationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recomendación no encontrada: " + id));
    }

    public void delete(Long curatorId, Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recomendación no encontrada: " + recommendationId));
        if (!recommendation.getCurator().getId().equals(curatorId)) {
            throw new IllegalArgumentException(
                    "No tienes permiso para eliminar esta recomendación");
        }
        recommendationRepository.delete(recommendation);
        recalculateCriticPoints(recommendation.getCurator());
    }

    private void recalculateCriticPoints(User curator) {
        double total = recommendationRepository
                .findByCuratorIdOrderByCreatedAtDesc(curator.getId())
                .stream()
                .mapToDouble(Recommendation::getContribution)
                .sum();
        curator.setCriticPoints(total);
    }

    private RecommendationResponse toResponse(Recommendation r) {
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