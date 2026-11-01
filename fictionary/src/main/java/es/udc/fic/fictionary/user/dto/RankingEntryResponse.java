package es.udc.fic.fictionary.user.dto;

public record RankingEntryResponse(
        Long id,
        String username,
        String bio,
        String avatarUrl,
        double criticPoints
) {}
