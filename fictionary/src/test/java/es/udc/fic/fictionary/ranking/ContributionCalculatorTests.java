package es.udc.fic.fictionary.ranking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica el modelo de Critic Points descrito en el capítulo de diseño del ranking. */
class ContributionCalculatorTests {

    private final ContributionCalculator w1 =
            new ContributionCalculator(RankingParameters.defaults(), new LogInverseWeight());

    @Test
    void reproducesTheNumericExampleOfTheDesignChapter() {
        // Caso A: título popular (π = 100), 10 valoraciones con media 8
        assertEquals(0.297, w1.contribution(10, 80, PopularityInfo.of(100)), 0.001);
        // Caso B: título poco popular (π = 2), mismas valoraciones
        assertEquals(0.794, w1.contribution(10, 80, PopularityInfo.of(2)), 0.001);
        // Caso C: poco popular, solo 2 valoraciones con media 10
        assertEquals(0.613, w1.contribution(2, 20, PopularityInfo.of(2)), 0.001);
    }

    @Test
    void lessPopularTitleContributesMoreWithSameRatings() {
        double popular = w1.contribution(10, 80, PopularityInfo.of(100));
        double niche = w1.contribution(10, 80, PopularityInfo.of(2));
        assertTrue(niche > popular);
    }

    @Test
    void fewRatingsContributeLessThanManyWithTheSameQualitySignal() {
        double fewRatings = w1.contribution(2, 20, PopularityInfo.of(2));
        double manyRatings = w1.contribution(10, 80, PopularityInfo.of(2));
        assertTrue(fewRatings < manyRatings);
    }

    @Test
    void noRatingsContributeNothing() {
        assertEquals(0.0, w1.contribution(0, 0, PopularityInfo.of(2)), 0.0);
    }

    @Test
    void neutralAverageContributesNothing() {
        // Media 5,5 con 10 valoraciones: suma = 55
        assertEquals(0.0, w1.contribution(10, 55, PopularityInfo.of(2)), 1e-12);
    }

    @Test
    void lowRatingsContributeNegativePoints() {
        assertTrue(w1.contribution(10, 20, PopularityInfo.of(2)) < 0);
    }

    @Test
    void contributionIsBoundedByWeightTimesMaximumDeviation() {
        PopularityInfo info = PopularityInfo.of(0);
        double weight = new LogInverseWeight().weight(info);
        double best = w1.contribution(1_000_000, 10_000_000L, info);
        double worst = w1.contribution(1_000_000, 1_000_000L, info);
        assertTrue(best < 4.5 * weight);
        assertTrue(worst > -4.5 * weight);
    }

    @Test
    void baselineWithoutWeightingIgnoresPopularity() {
        ContributionCalculator w0 =
                new ContributionCalculator(RankingParameters.defaults(), new NoWeight());
        assertEquals(w0.contribution(10, 80, PopularityInfo.of(100)),
                w0.contribution(10, 80, PopularityInfo.of(2)), 1e-12);
    }

    @Test
    void compactFormMatchesTheLambdaDeltaForm() {
        long n = 7;
        long sum = 49;
        double pi = 12.3;
        double lambda = n / (n + 5.0);
        double delta = (double) sum / n - 5.5;
        double expected = new LogInverseWeight().weight(PopularityInfo.of(pi)) * lambda * delta;
        assertEquals(expected, w1.contribution(n, sum, PopularityInfo.of(pi)), 1e-12);
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RankingParameters(0.5, 5));
        assertThrows(IllegalArgumentException.class, () -> new RankingParameters(5.5, 0));
        assertThrows(IllegalArgumentException.class, () -> w1.contribution(-1, 0, PopularityInfo.of(1)));
    }
}
