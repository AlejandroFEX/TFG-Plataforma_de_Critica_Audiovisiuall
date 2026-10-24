package es.udc.fic.fictionary.ranking;

/**
 * Calcula la contribución x_r de una recomendación a los Critic Points de su curador:
 *
 * <pre>
 *   x_r = w(pi_r) * (S_r - n_r * s0) / (n_r + k)      (x_r = 0 si n_r = 0)
 * </pre>
 *
 * <p>Es un servicio de dominio puro, sin dependencias de Spring (RNF-MAN-02), de modo que el
 * simulador y la aplicación web comparten exactamente el mismo cálculo.
 */
public final class ContributionCalculator {

    private final RankingParameters parameters;
    private final PopularityWeight weight;

    public ContributionCalculator(RankingParameters parameters, PopularityWeight weight) {
        if (parameters == null || weight == null) {
            throw new IllegalArgumentException("Parámetros y función de ponderación son obligatorios");
        }
        this.parameters = parameters;
        this.weight = weight;
    }

    /**
     * @param ratingCount n_r, número de valoraciones
     * @param ratingSum   S_r, suma de puntuaciones
     * @param popularity  popularidad del título al recomendar
     */
    public double contribution(long ratingCount, long ratingSum, PopularityInfo popularity) {
        if (ratingCount < 0) {
            throw new IllegalArgumentException("El número de valoraciones no puede ser negativo");
        }
        if (ratingCount == 0) {
            return 0.0;
        }
        double deviationSum = ratingSum - ratingCount * parameters.neutralScore();
        double satisfaction = deviationSum / (ratingCount + parameters.priorWeight());
        return weight.weight(popularity) * satisfaction;
    }

    public PopularityWeight weightFunction() {
        return weight;
    }

    public RankingParameters parameters() {
        return parameters;
    }
}
