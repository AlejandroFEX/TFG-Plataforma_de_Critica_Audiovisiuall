package es.udc.fic.fictionary.recommendation.dto;

import es.udc.fic.fictionary.catalog.MediaType;

import java.time.Instant;

public record RecommendationResponse(
        Long id,
        Long curatorId,
        String curatorUsername,
        double curatorCriticPoints,
        Long titleId,
        long tmdbId,
        MediaType mediaType,
        String titleName,
        String posterUrl,
        double popularitySnapshot,
        boolean hiddenGem,
        String text,
        int ratingCount,
        double averageRating,
        double contribution,
        Instant createdAt
) {}