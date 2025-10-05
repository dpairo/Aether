package com.aether.app.infrastructure.web.dto;

/**
 * DTO representing a time slot with air quality prediction
 */
public record BestTimeSlotDTO(
        String timeRange,
        Integer predictedAQI,
        String aqiColor,
        String recommendation
) {}

