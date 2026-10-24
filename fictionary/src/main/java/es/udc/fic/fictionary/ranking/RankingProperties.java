package es.udc.fic.fictionary.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Propiedades {@code fictionary.ranking.*} (RF-CRP-08: función de ponderación configurable). */
@ConfigurationProperties(prefix = "fictionary.ranking")
public record RankingProperties(
        @DefaultValue("5.5") double neutralScore,
        @DefaultValue("5") double priorWeight,
        @DefaultValue("LOG_INVERSE") WeightFunction weightFunction,
        @DefaultValue("0.5") double powerAlpha,
        @DefaultValue("0.1") double percentileMinWeight,
        @DefaultValue("2") double percentileBeta) {
}
