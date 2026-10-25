package es.udc.fic.fictionary.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RatingRequest(

        @NotNull(message = "La puntuación es obligatoria")
        @Min(value = 1, message = "La puntuación mínima es 1")
        @Max(value = 10, message = "La puntuación máxima es 10")
        Integer score
) {}