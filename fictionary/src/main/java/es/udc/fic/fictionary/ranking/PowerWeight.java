package es.udc.fic.fictionary.ranking;

/** W2: w(π) = (1 + π)^(-α), con α en (0, 1]. */
public final class PowerWeight implements PopularityWeight {

    private final double alpha;

    public PowerWeight(double alpha) {
        if (!(alpha > 0 && alpha <= 1)) {
            throw new IllegalArgumentException("alpha debe estar en el intervalo (0, 1]");
        }
        this.alpha = alpha;
    }

    @Override
    public double weight(PopularityInfo info) {
        return Math.pow(1.0 + info.popularity(), -alpha);
    }

    @Override
    public String id() {
        return "W2";
    }
}
