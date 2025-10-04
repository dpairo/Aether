package com.aether.app.infrastructure.web.dto;

import java.util.List;

/**
 * DTO representing the response containing pollution hotspots
 */
public record PollutionHotspotsResponseDTO(
        Double centerLatitude,
        Double centerLongitude,
        Integer radiusMeters,
        Integer totalLocationsFound,
        List<PollutionHotspotDTO> hotspots,
        String message
) {}

