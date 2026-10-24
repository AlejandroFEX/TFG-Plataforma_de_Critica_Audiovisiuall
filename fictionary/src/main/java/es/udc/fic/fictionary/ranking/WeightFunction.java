package es.udc.fic.fictionary.ranking;

/** Variante de ponderación seleccionable por configuración. */
public enum WeightFunction {
    /** W0: sin ponderar (línea base). */
    NONE,
    /** W1: decrecimiento logarítmico. */
    LOG_INVERSE,
    /** W2: potencial. */
    POWER,
    /** W3: basada en el percentil de popularidad. */
    PERCENTILE
}
