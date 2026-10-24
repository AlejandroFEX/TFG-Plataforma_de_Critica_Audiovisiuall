package es.udc.fic.fictionary.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByRaterIdAndRecommendationId(Long raterId, Long recommendationId);
}
