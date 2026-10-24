package es.udc.fic.fictionary.ranking;

/** W1: w(π) = 1 / (1 + ln(1 + π)). */
public final class LogInverseWeight implements PopularityWeight {

    @Override
    public double weight(PopularityInfo info) {
        return 1.0 / (1.0 + Math.log1p(info.popularity()));
    }

    @Override
    public String id() {
        return "W1";
    }
}
