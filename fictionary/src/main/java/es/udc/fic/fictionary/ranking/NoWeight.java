package es.udc.fic.fictionary.ranking;

/** W0: línea base sin ponderar, w = 1. */
public final class NoWeight implements PopularityWeight {

    @Override
    public double weight(PopularityInfo info) {
        return 1.0;
    }

    @Override
    public String id() {
        return "W0";
    }
}
