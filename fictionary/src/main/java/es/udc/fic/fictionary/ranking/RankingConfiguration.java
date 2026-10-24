package es.udc.fic.fictionary.ranking;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Construye el motor de ranking a partir de la configuración. */
@Configuration
@EnableConfigurationProperties(RankingProperties.class)
public class RankingConfiguration {

    @Bean
    RankingParameters rankingParameters(RankingProperties properties) {
        return new RankingParameters(properties.neutralScore(), properties.priorWeight());
    }

    @Bean
    PopularityWeight popularityWeight(RankingProperties properties) {
        return switch (properties.weightFunction()) {
            case NONE -> new NoWeight();
            case LOG_INVERSE -> new LogInverseWeight();
            case POWER -> new PowerWeight(properties.powerAlpha());
            case PERCENTILE -> new PercentileWeight(properties.percentileMinWeight(), properties.percentileBeta());
        };
    }

    @Bean
    ContributionCalculator contributionCalculator(RankingParameters parameters, PopularityWeight weight) {
        return new ContributionCalculator(parameters, weight);
    }
}
