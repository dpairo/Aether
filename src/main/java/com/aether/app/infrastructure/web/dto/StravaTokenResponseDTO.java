package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing Strava OAuth token response
 */
public record StravaTokenResponseDTO(
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_at") Long expiresAt,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("access_token") String accessToken,
        StravaAthleteDTO athlete
) {}


