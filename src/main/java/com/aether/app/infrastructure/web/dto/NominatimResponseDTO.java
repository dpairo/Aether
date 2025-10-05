package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the response from Nominatim API (OpenStreetMap)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResponseDTO(
        @JsonProperty("place_id") Long placeId,
        @JsonProperty("licence") String licence,
        @JsonProperty("osm_type") String osmType,
        @JsonProperty("osm_id") Long osmId,
        @JsonProperty("lat") String lat,
        @JsonProperty("lon") String lon,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("name") String name,
        @JsonProperty("address") NominatimAddressDTO address,
        @JsonProperty("boundingbox") String[] boundingbox
) {
    /**
     * DTO for address details in Nominatim response
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NominatimAddressDTO(
            @JsonProperty("city") String city,
            @JsonProperty("town") String town,
            @JsonProperty("village") String village,
            @JsonProperty("municipality") String municipality,
            @JsonProperty("province") String province,
            @JsonProperty("state") String state,
            @JsonProperty("country") String country,
            @JsonProperty("country_code") String countryCode,
            @JsonProperty("postcode") String postcode
    ) {}
}

