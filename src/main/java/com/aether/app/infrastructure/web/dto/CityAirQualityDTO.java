package com.aether.app.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CityAirQualityDTO(
        String city,
        String cityId,
        Double latitude,
        Double longitude,
        Integer aqi,
        String aqiStatus,
        String dominantPollutant,
        AirQualityDataDTO airQuality,
        String timestamp
) {
    public static CityAirQualityDTO fromWAQIResponse(String city, String cityId, Double lat, Double lon, WAQIResponseDTO response) {
        return new CityAirQualityDTO(
                city,
                cityId,
                lat,
                lon,
                response.data().aqi(),
                getAQIStatus(response.data().aqi()),
                response.data().dominentpol(),
                new AirQualityDataDTO(
                        response.data().iaqi().pm25() != null ? response.data().iaqi().pm25().v() : null,
                        response.data().iaqi().pm10() != null ? response.data().iaqi().pm10().v() : null,
                        response.data().iaqi().no2() != null ? response.data().iaqi().no2().v() : null,
                        response.data().iaqi().o3() != null ? response.data().iaqi().o3().v() : null,
                        response.data().iaqi().co() != null ? response.data().iaqi().co().v() : null,
                        response.data().iaqi().so2() != null ? response.data().iaqi().so2().v() : null
                ),
                response.data().time().iso()
        );
    }

    private static String getAQIStatus(Integer aqi) {
        if (aqi == null) return "Unknown";
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }
}
