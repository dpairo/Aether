package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing an OpenAQ location with pollution data
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAQLocationDTO(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("country") String country,
        @JsonProperty("city") String city,
        @JsonProperty("measurements") Integer measurements,
        @JsonProperty("lastUpdated") String lastUpdated,
        @JsonProperty("parameters") java.util.List<ParameterDTO> parameters
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParameterDTO(
            @JsonProperty("id") Integer id,
            @JsonProperty("parameter") String parameter,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("lastValue") Double lastValue,
            @JsonProperty("unit") String unit
    ) {}
}

