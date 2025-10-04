package com.aether.app.infrastructure.web.controller;

import com.aether.app.air.AirQualityService;
import com.aether.app.air.OpenAQService;
import com.aether.app.infrastructure.web.dto.CityAirQualityDTO;
import com.aether.app.infrastructure.web.dto.PollutionHotspotsResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AirController {

    @Autowired
    private AirQualityService airQualityService;
    
    @Autowired
    private OpenAQService openAQService;

    /**
     * Get air quality data for a specific Spanish city
     * @param cityId The city identifier (e.g., "madrid", "barcelona", "valencia")
     * @return Air quality data for the city with AQI and color information
     */
    @GetMapping("/air/quality/city/{cityId}")
    public CityAirQualityDTO getCityAirQuality(@PathVariable String cityId) {
        return airQualityService.getAirQualityByCity(cityId);
    }
    
    /**
     * Get pollution hotspots near a location using OpenAQ data
     * @param lat Latitude of the center point
     * @param lon Longitude of the center point
     * @param radius Search radius in meters (optional, default: 500m)
     * @param limit Maximum number of hotspots to return (optional, default: 3)
     * @return List of the most polluted locations in the area
     */
    @GetMapping("/air/quality/hotspots")
    public PollutionHotspotsResponseDTO getPollutionHotspots(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) Integer limit
    ) {
        return openAQService.getPollutionHotspots(lat, lon, radius, limit);
    }
}