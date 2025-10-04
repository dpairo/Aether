package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * DTO representing a Strava activity
 */
public record StravaActivityDTO(
        Long id,
        String name,
        String type,
        @JsonProperty("start_date") String startDate,
        @JsonProperty("start_date_local") String startDateLocal,
        Double distance,
        @JsonProperty("moving_time") Integer movingTime,
        @JsonProperty("elapsed_time") Integer elapsedTime,
        @JsonProperty("total_elevation_gain") Double totalElevationGain,
        @JsonProperty("start_latlng") List<Double> startLatLng,
        @JsonProperty("end_latlng") List<Double> endLatLng,
        @JsonProperty("location_city") String locationCity,
        @JsonProperty("location_state") String locationState,
        @JsonProperty("location_country") String locationCountry,
        @JsonProperty("map") MapDTO map
) {
    public record MapDTO(
            String id,
            @JsonProperty("summary_polyline") String summaryPolyline,
            @JsonProperty("polyline") String polyline
    ) {}
}


