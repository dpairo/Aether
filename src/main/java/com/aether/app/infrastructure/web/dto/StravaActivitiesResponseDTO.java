package com.aether.app.infrastructure.web.dto;

import java.util.List;

/**
 * DTO for activities found in a specific location
 */
public record StravaActivitiesResponseDTO(
        Long athleteId,
        String city,
        String state,
        String country,
        Integer totalActivities,
        List<StravaActivityDTO> activities,
        String message
) {}


