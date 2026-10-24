package es.udc.fic.fictionary.user.dto;

public record AuthResponse (
    Long userId,
    String username,
    String email,
    String token
) {}
