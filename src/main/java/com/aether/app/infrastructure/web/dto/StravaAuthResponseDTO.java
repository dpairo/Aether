package com.aether.app.infrastructure.web.dto;

/**
 * DTO sent to frontend after successful Strava authentication
 */
public record StravaAuthResponseDTO(
        Long athleteId,
        String firstName,
        String lastName,
        String username,
        String accessToken,
        Long expiresAt,
        String message
) {}

