package com.aether.app.infrastructure.web.dto;

/**
 * DTO representing a pollution hotspot for the frontend
 */
public record PollutionHotspotDTO(
        String locationName,
        Double latitude,
        Double longitude,
        Double pm25Value,
        String unit,
        Integer aqi,
        String aqiStatus,
        String aqiColor,
        String lastUpdated
) {}

