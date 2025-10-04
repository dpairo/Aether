package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the OpenAQ API response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAQResponseDTO(
        @JsonProperty("results") java.util.List<OpenAQLocationDTO> results
) {}

