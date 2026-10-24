package es.udc.fic.fictionary.recommendation;

import es.udc.fic.fictionary.catalog.MediaType;
import es.udc.fic.fictionary.catalog.Title;
import es.udc.fic.fictionary.ranking.ContributionCalculator;
import es.udc.fic.fictionary.ranking.LogInverseWeight;
import es.udc.fic.fictionary.ranking.PopularityInfo;
import es.udc.fic.fictionary.ranking.RankingParameters;
import es.udc.fic.fictionary.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationTests {

    private final User curator = new User("curador", "curador@example.com", "hash");
    private final User rater = new User("valorador", "valorador@example.com", "hash");
    private final Title title = new Title(603, MediaType.MOVIE, "Titulo", 2.0, 40);

    private Recommendation newRecommendation() {
        return new Recommendation(curator, title, "Merece la pena por su atmósfera.");
    }

    @Test
    void takesThePopularitySnapshotFromTheTitle() {
        Recommendation recommendation = newRecommendation();
        assertEquals(2.0, recommendation.getPopularitySnapshot(), 0.0);
        assertEquals(40, recommendation.getVoteCountSnapshot());
        assertFalse(recommendation.isPopularityImputed());
    }

    @Test
    void snapshotDoesNotChangeWhenTheTitlePopularityChanges() {
        Recommendation recommendation = newRecommendation();
        title.refreshPopularity(900.0, 5000);
        assertEquals(2.0, recommendation.getPopularitySnapshot(), 0.0);
        assertEquals(40, recommendation.getVoteCountSnapshot());
    }

    @Test
    void rejectsInvalidText() {
        assertThrows(IllegalArgumentException.class, () -> new Recommendation(curator, title, "   "));
        assertThrows(IllegalArgumentException.class, () -> new Recommendation(curator, title, null));
        String tooLong = "x".repeat(Recommendation.MAX_TEXT_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> new Recommendation(curator, title, tooLong));
    }

    @Test
    void maintainsAggregatesOnAddChangeAndRemove() {
        Recommendation recommendation = newRecommendation();
        recommendation.addRating(8);
        recommendation.addRating(6);
        assertEquals(2, recommendation.getRatingCount());
        assertEquals(14L, recommendation.getRatingSum());
        assertEquals(7.0, recommendation.averageRating(), 1e-12);

        recommendation.changeRating(6, 10);
        assertEquals(2, recommendation.getRatingCount());
        assertEquals(18L, recommendation.getRatingSum());

        recommendation.removeRating(8);
        assertEquals(1, recommendation.getRatingCount());
        assertEquals(10L, recommendation.getRatingSum());
    }

    @Test
    void rejectsInvalidScoresAndOperationsOnEmptyAggregates() {
        Recommendation recommendation = newRecommendation();
        assertThrows(IllegalArgumentException.class, () -> recommendation.addRating(0));
        assertThrows(IllegalArgumentException.class, () -> recommendation.addRating(11));
        assertThrows(IllegalStateException.class, () -> recommendation.removeRating(5));
        assertThrows(IllegalStateException.class, () -> recommendation.changeRating(5, 6));
    }

    @Test
    void ratingRejectsSelfRatingAndInvalidScore() {
        Recommendation recommendation = newRecommendation();
        assertThrows(IllegalArgumentException.class, () -> new Rating(curator, recommendation, 5));
        assertThrows(IllegalArgumentException.class, () -> new Rating(rater, recommendation, 11));
        Rating rating = new Rating(rater, recommendation, 7);
        assertEquals(7, rating.getScore());
        assertEquals(7, rating.changeScore(9));
        assertEquals(9, rating.getScore());
    }

    /**
     * IN-09: el valor mantenido de forma incremental coincide con el recálculo completo
     * desde las valoraciones (RNF-REP-01).
     */
    @Test
    void incrementalCriticPointsMatchFullRecalculation() {
        ContributionCalculator calculator =
                new ContributionCalculator(RankingParameters.defaults(), new LogInverseWeight());
        Recommendation recommendation = newRecommendation();
        PopularityInfo info = PopularityInfo.of(recommendation.getPopularitySnapshot());

        double criticPoints = 0.0;
        int[] scores = {8, 6, 9, 3, 7, 10, 5, 8, 8, 2};
        for (int score : scores) {
            recommendation.addRating(score);
            double next = calculator.contribution(
                    recommendation.getRatingCount(), recommendation.getRatingSum(), info);
            criticPoints += next - recommendation.getContribution();
            recommendation.setContribution(next);
        }

        // Cambio y retirada de valoraciones
        recommendation.changeRating(3, 9);
        double afterChange = calculator.contribution(
                recommendation.getRatingCount(), recommendation.getRatingSum(), info);
        criticPoints += afterChange - recommendation.getContribution();
        recommendation.setContribution(afterChange);

        recommendation.removeRating(2);
        double afterRemoval = calculator.contribution(
                recommendation.getRatingCount(), recommendation.getRatingSum(), info);
        criticPoints += afterRemoval - recommendation.getContribution();
        recommendation.setContribution(afterRemoval);

        // Recálculo completo con las puntuaciones finales
        int[] finalScores = {8, 6, 9, 9, 7, 10, 5, 8, 8};
        long sum = 0;
        for (int s : finalScores) {
            sum += s;
        }
        double full = calculator.contribution(finalScores.length, sum, info);

        assertEquals(full, criticPoints, 1e-9);
        assertTrue(criticPoints > 0);
    }
}
