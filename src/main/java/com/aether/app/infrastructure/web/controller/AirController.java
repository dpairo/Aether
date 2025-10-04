package com.aether.app.infrastructure.web.controller;

import com.aether.app.air.AirQualityService;
import com.aether.app.infrastructure.web.dto.CityAirQualityDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AirController {

    @Autowired
    private AirQualityService airQualityService;

    /**
     * Get air quality data for a specific Spanish city
     * @param cityId The city identifier (e.g., "madrid", "barcelona", "valencia")
     * @return Air quality data for the city with AQI and color information
     */
    @GetMapping("/air/quality/city/{cityId}")
    public CityAirQualityDTO getCityAirQuality(@PathVariable String cityId) {
        return airQualityService.getAirQualityByCity(cityId);
    }
}