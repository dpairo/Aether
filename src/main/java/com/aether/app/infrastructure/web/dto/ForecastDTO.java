package com.aether.app.infrastructure.web.dto;

public record ForecastDTO(
        double lat,
        double lon,
        String timestamp,
        String pollutant,
        Double value
) {}