package es.udc.fic.fictionary.social.dto;

public record UserSummaryResponse(
        Long id,
        String username,
        String avatarUrl,
        double criticPoints
) {}