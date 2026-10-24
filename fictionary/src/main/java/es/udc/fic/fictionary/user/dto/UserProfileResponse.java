package es.udc.fic.fictionary.user.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String bio,
        double criticPoints,
        Instant createdAt
) {}