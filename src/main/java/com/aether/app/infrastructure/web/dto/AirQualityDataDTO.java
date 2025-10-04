package com.aether.app.infrastructure.web.dto;

public record AirQualityDataDTO(
        Double pm25,
        Double pm10,
        Double no2,
        Double o3,
        Double co,
        Double so2
) {}
