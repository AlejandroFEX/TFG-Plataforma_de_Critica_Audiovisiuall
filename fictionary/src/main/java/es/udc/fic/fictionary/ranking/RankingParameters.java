package es.udc.fic.fictionary.ranking;

/**
 * Parámetros del modelo de Critic Points.
 *
 * @param neutralScore puntuación neutra s0 (por defecto 5,5, punto medio de la escala 1-10)
 * @param priorWeight  peso del conocimiento previo k, en pseudovaloraciones neutras (por defecto 5)
 */
public record RankingParameters(double neutralScore, double priorWeight) {

    public static final double DEFAULT_NEUTRAL_SCORE = 5.5;
    public static final double DEFAULT_PRIOR_WEIGHT = 5.0;

    public RankingParameters {
        if (neutralScore < 1 || neutralScore > 10) {
            throw new IllegalArgumentException("La puntuación neutra debe estar entre 1 y 10");
        }
        if (!(priorWeight > 0)) {
            throw new IllegalArgumentException("El peso previo k debe ser mayor que 0");
        }
    }

    public static RankingParameters defaults() {
        return new RankingParameters(DEFAULT_NEUTRAL_SCORE, DEFAULT_PRIOR_WEIGHT);
    }
}
