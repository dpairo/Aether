package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for city search response containing coordinates and display information
 */
public record CitySearchDTO(
        @JsonProperty("city") String city,
        @JsonProperty("latitude") String latitude,
        @JsonProperty("longitude") String longitude,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("boundingBox") String[] boundingBox
) {}

