package es.udc.fic.fictionary.recommendation;

import es.udc.fic.fictionary.catalog.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    boolean existsByCuratorIdAndTitleId(Long curatorId, Long titleId);

    List<Recommendation> findByCuratorIdOrderByCreatedAtDesc(Long curatorId);

    @Query("select r from Recommendation r where r.curator.id in :curatorIds order by r.createdAt desc")
    List<Recommendation> findFeedByCuratorIds(@Param("curatorIds") List<Long> curatorIds);

    @Query("select r from Recommendation r where r.curator.id in :curatorIds " +
           "and (:mediaType is null or r.title.mediaType = :mediaType) " +
           "and (:maxPopularity is null or r.popularitySnapshot <= :maxPopularity) " +
           "order by r.curator.criticPoints desc")
    List<Recommendation> findFeedFiltered(
            @Param("curatorIds") List<Long> curatorIds,
            @Param("mediaType") MediaType mediaType,
            @Param("maxPopularity") Double maxPopularity);
}