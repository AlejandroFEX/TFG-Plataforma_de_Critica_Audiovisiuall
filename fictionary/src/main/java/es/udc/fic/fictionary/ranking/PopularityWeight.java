package es.udc.fic.fictionary.ranking;

/**
 * Función de ponderación por popularidad w(·). Debe ser no creciente respecto de la popularidad,
 * positiva y acotada en (0, 1].
 */
public interface PopularityWeight {

    double weight(PopularityInfo info);

    /** Identificador de la variante (W0..W3) para registrar experimentos. */
    String id();
}
