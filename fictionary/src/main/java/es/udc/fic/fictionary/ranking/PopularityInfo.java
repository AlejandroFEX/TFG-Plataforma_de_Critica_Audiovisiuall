package es.udc.fic.fictionary.ranking;

/**
 * Información de popularidad de un título en el momento de la recomendación.
 *
 * @param popularity popularidad bruta (&ge; 0), p. ej. la de TMDB
 * @param percentile percentil de la popularidad dentro del catálogo, en [0, 1];
 *                   {@code NaN} si no está disponible (solo lo exige la variante W3)
 */
public record PopularityInfo(double popularity, double percentile) {

    public PopularityInfo {
        if (Double.isNaN(popularity) || popularity < 0) {
            throw new IllegalArgumentException("La popularidad debe ser un número mayor o igual que 0");
        }
        if (!Double.isNaN(percentile) && (percentile < 0 || percentile > 1)) {
            throw new IllegalArgumentException("El percentil debe estar en el intervalo [0, 1]");
        }
    }

    /** Información sin percentil. */
    public static PopularityInfo of(double popularity) {
        return new PopularityInfo(popularity, Double.NaN);
    }

    public boolean hasPercentile() {
        return !Double.isNaN(percentile);
    }
}
