package es.udc.fic.fictionary.ranking;

/** W3: w(q) = wMin + (1 - wMin) (1 - q)^β, con q el percentil de popularidad en [0, 1]. */
public final class PercentileWeight implements PopularityWeight {

    private final double minWeight;
    private final double beta;

    public PercentileWeight(double minWeight, double beta) {
        if (!(minWeight > 0 && minWeight <= 1)) {
            throw new IllegalArgumentException("minWeight debe estar en el intervalo (0, 1]");
        }
        if (!(beta > 0)) {
            throw new IllegalArgumentException("beta debe ser mayor que 0");
        }
        this.minWeight = minWeight;
        this.beta = beta;
    }

    @Override
    public double weight(PopularityInfo info) {
        if (!info.hasPercentile()) {
            throw new IllegalArgumentException("La variante W3 requiere el percentil de popularidad");
        }
        return minWeight + (1.0 - minWeight) * Math.pow(1.0 - info.percentile(), beta);
    }

    @Override
    public String id() {
        return "W3";
    }
}
