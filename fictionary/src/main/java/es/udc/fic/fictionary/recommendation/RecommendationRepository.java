package es.udc.fic.fictionary.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    boolean existsByCuratorIdAndTitleId(Long curatorId, Long titleId);

    List<Recommendation> findByCuratorIdOrderByCreatedAtDesc(Long curatorId);
}
