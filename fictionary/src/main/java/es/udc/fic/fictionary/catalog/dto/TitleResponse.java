package es.udc.fic.fictionary.catalog.dto;

import es.udc.fic.fictionary.catalog.MediaType;

import java.time.LocalDate;
import java.util.Set;

public record TitleResponse(
        Long id,
        long tmdbId,
        MediaType mediaType,
        String name,
        String overview,
        String posterUrl,
        LocalDate releaseDate,
        double popularity,
        int voteCount,
        boolean hiddenGem,
        Set<String> genres
) {}