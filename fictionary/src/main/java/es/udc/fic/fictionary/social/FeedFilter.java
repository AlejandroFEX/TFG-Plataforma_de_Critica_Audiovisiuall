package es.udc.fic.fictionary.social;

import es.udc.fic.fictionary.catalog.MediaType;

import java.time.LocalDate;

/** Filtros y orden opcionales del feed. Los valores nulos no filtran. */
public record FeedFilter(
        MediaType mediaType,
        Integer genre,          // id de género de TMDB
        Double minPopularity,   // popularidad >= valor
        Double maxPopularity,   // popularidad <  valor
        Double minRating,       // media de valoraciones >= valor
        LocalDate from,         // recomendaciones desde este día (incluido)
        LocalDate to,           // recomendaciones hasta este día (incluido)
        String sortBy           // recent | oldest | criticPoints | rating | leastPopular
) {}
