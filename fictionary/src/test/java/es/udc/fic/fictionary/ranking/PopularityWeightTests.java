package es.udc.fic.fictionary.ranking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopularityWeightTests {

    private static void assertNonIncreasingAndBounded(PopularityWeight weight) {
        double previous = Double.MAX_VALUE;
        for (double popularity = 0; popularity <= 10_000; popularity += 0.5) {
            double value = weight.weight(PopularityInfo.of(popularity));
            assertTrue(value > 0 && value <= 1, weight.id() + ": fuera de (0, 1] en " + popularity);
            assertTrue(value <= previous + 1e-15, weight.id() + ": no es no creciente en " + popularity);
            previous = value;
        }
    }

    @Test
    void logInverseIsNonIncreasingAndBounded() {
        assertNonIncreasingAndBounded(new LogInverseWeight());
    }

    @Test
    void powerIsNonIncreasingAndBounded() {
        assertNonIncreasingAndBounded(new PowerWeight(0.5));
        assertNonIncreasingAndBounded(new PowerWeight(1.0));
    }

    @Test
    void percentileIsNonIncreasingAndWithinBounds() {
        PercentileWeight weight = new PercentileWeight(0.1, 2);
        double previous = Double.MAX_VALUE;
        for (int i = 0; i <= 100; i++) {
            double value = weight.weight(new PopularityInfo(1, i / 100.0));
            assertTrue(value >= 0.1 - 1e-12 && value <= 1 + 1e-12);
            assertTrue(value <= previous + 1e-15);
            previous = value;
        }
    }

    @Test
    void zeroPopularityGivesMaximumWeight() {
        assertEquals(1.0, new LogInverseWeight().weight(PopularityInfo.of(0)), 1e-12);
        assertEquals(1.0, new PowerWeight(0.5).weight(PopularityInfo.of(0)), 1e-12);
        assertEquals(1.0, new NoWeight().weight(PopularityInfo.of(0)), 1e-12);
    }

    @Test
    void noWeightIgnoresPopularity() {
        NoWeight weight = new NoWeight();
        assertEquals(weight.weight(PopularityInfo.of(2)), weight.weight(PopularityInfo.of(5000)), 0.0);
    }

    @Test
    void percentileVariantRequiresPercentile() {
        PercentileWeight weight = new PercentileWeight(0.1, 2);
        assertThrows(IllegalArgumentException.class, () -> weight.weight(PopularityInfo.of(5)));
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PopularityInfo.of(-1));
        assertThrows(IllegalArgumentException.class, () -> new PopularityInfo(1, 1.5));
        assertThrows(IllegalArgumentException.class, () -> new PowerWeight(0));
        assertThrows(IllegalArgumentException.class, () -> new PowerWeight(1.5));
        assertThrows(IllegalArgumentException.class, () -> new PercentileWeight(0, 2));
        assertThrows(IllegalArgumentException.class, () -> new PercentileWeight(0.1, 0));
    }
}
