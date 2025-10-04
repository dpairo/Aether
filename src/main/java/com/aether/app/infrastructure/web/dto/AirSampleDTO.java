package com.aether.app.infrastructure.web.dto;

public record AirSampleDTO(
        Long stationId,
        String timestamp,
        Double pm25,
        Double no2,
        Double o3,
        Integer aqi,
        String aqiColor
) {
    public AirSampleDTO(Long stationId, String timestamp, Double pm25, Double no2, Double o3, Integer aqi) {
        this(stationId, timestamp, pm25, no2, o3, aqi, AQIColorUtil.getAQIColor(aqi));
    }
}