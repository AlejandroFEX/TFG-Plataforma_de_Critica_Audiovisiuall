package es.udc.fic.fictionary.recommendation.dto;

import es.udc.fic.fictionary.catalog.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecommendationRequest(

        @NotNull(message = "El tmdbId es obligatorio")
        Long tmdbId,

        @NotNull(message = "El tipo de contenido es obligatorio")
        MediaType mediaType,

        @NotBlank(message = "El texto es obligatorio")
        @Size(min = 10, max = 2000, message = "El texto debe tener entre 10 y 2000 caracteres")
        String text
) {}