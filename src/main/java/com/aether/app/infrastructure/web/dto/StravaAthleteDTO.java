package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing Strava athlete information
 */
public record StravaAthleteDTO(
        Long id,
        String username,
        @JsonProperty("firstname") String firstName,
        @JsonProperty("lastname") String lastName,
        String city,
        String state,
        String country,
        String sex,
        String profile,
        @JsonProperty("profile_medium") String profileMedium
) {}


