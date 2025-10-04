package com.aether.app.infrastructure.web.dto;

/**
 * DTO for Strava authentication errors
 */
public record StravaErrorDTO(
        String error,
        String message
) {}

