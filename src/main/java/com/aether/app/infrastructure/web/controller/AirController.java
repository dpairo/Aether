package com.aether.app.infrastructure.web.controller;

import com.aether.app.air.AirQualityService;
import com.aether.app.infrastructure.web.dto.AirSampleDTO;
import com.aether.app.infrastructure.web.dto.CityAirQualityDTO;
import com.aether.app.infrastructure.web.dto.ForecastDTO;
import com.aether.app.infrastructure.web.dto.ProvinceAirQualityDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AirController {

    @Autowired
    private AirQualityService airQualityService;

    @GetMapping("/air/samples")
    public List<AirSampleDTO> samples(
            @RequestParam Long stationId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return List.of(
                new AirSampleDTO(stationId, "2025-10-04T10:00:00Z", 12.3, 40.1, 35.0, 52),
                new AirSampleDTO(stationId, "2025-10-04T11:00:00Z", 13.0, 39.2, 34.8, 51),
                new AirSampleDTO(stationId, "2025-10-04T12:00:00Z", 15.1, 38.0, 33.5, 55)
        );
    }

    @GetMapping("/air/forecast")
    public ForecastDTO forecast(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        return new ForecastDTO(lat, lon, "2025-10-04T18:00:00Z", "PM2_5", 17.2);
    }

    /**
     * Get air quality data for a specific Spanish city
     * @param cityId The city identifier (e.g., "madrid", "barcelona")
     * @return Air quality data for the city
     */
    @GetMapping("/air/quality/city/{cityId}")
    public CityAirQualityDTO getCityAirQuality(@PathVariable String cityId) {
        return airQualityService.getAirQualityByCity(cityId);
    }

    /**
     * Get air quality data for all Spanish cities
     * This endpoint is designed for frontend map visualization
     * @return List of air quality data for all Spanish cities
     */
    @GetMapping("/air/quality/cities")
    public List<CityAirQualityDTO> getAllCitiesAirQuality() {
        return airQualityService.getAllSpanishCitiesAirQuality();
    }

    /**
     * Get air quality data for all Spanish provinces
     * This endpoint provides province-level AQI data for map visualization
     * @return List of air quality data for all Spanish provinces
     */
    @GetMapping("/air/quality/provinces")
    public List<ProvinceAirQualityDTO> getAllProvincesAirQuality() {
        return airQualityService.getAllSpanishProvincesAirQuality();
    }

    /**
     * Get air quality data for cities within a specific province
     * @param provinceCode The province code (e.g., "M" for Madrid, "B" for Barcelona)
     * @return List of air quality data for cities in the specified province
     */
    @GetMapping("/air/quality/province/{provinceCode}/cities")
    public List<CityAirQualityDTO> getCitiesByProvince(@PathVariable String provinceCode) {
        return airQualityService.getCitiesByProvince(provinceCode);
    }
}