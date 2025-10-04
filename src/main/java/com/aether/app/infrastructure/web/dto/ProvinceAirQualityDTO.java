package com.aether.app.infrastructure.web.dto;

public record ProvinceAirQualityDTO(
        String province,
        String provinceCode,
        Double latitude,
        Double longitude,
        Integer aqi,
        String aqiStatus,
        String aqiColor,
        String dominantPollutant,
        AirQualityDataDTO airQuality,
        String timestamp
) {
    public static ProvinceAirQualityDTO fromCityData(String province, String provinceCode, 
            Double lat, Double lon, CityAirQualityDTO cityData) {
        return new ProvinceAirQualityDTO(
                province,
                provinceCode,
                lat,
                lon,
                cityData.aqi(),
                cityData.aqiStatus(),
                cityData.aqiColor(),
                cityData.dominantPollutant(),
                cityData.airQuality(),
                cityData.timestamp()
        );
    }
}
