package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a GeoJSON Feature Collection for routes
 */
public record RouteGeoJsonDTO(
        String type,
        List<RouteFeature> features,
        RouteMetadata metadata
) {
    public static RouteGeoJsonDTO create(List<RouteFeature> features, RouteMetadata metadata) {
        return new RouteGeoJsonDTO("FeatureCollection", features, metadata);
    }
    
    public record RouteFeature(
            String type,
            RouteGeometry geometry,
            RouteProperties properties
    ) {
        public static RouteFeature create(List<List<Double>> coordinates, RouteProperties properties) {
            return new RouteFeature("Feature", new RouteGeometry("LineString", coordinates), properties);
        }
    }
    
    public record RouteGeometry(
            String type,
            List<List<Double>> coordinates
    ) {}
    
    public record RouteProperties(
            Long activityId,
            String name,
            String type,
            Double distance,
            Integer movingTime,
            String startDate,
            Integer repetitions,
            String color,
            @JsonProperty("location_city") String locationCity
    ) {}
    
    public record RouteMetadata(
            Long athleteId,
            String city,
            Integer totalRoutes,
            Integer totalRepetitions,
            String message
    ) {}
}

