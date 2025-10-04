package com.aether.app.air;

import com.aether.app.infrastructure.web.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to fetch pollution hotspots using WAQI API (geo feed)
 */
@Service
public class OpenAQService {

    private static final Logger log = LoggerFactory.getLogger(OpenAQService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${air-quality.waqi.base-url}")
    private String baseUrl;
    
    @Value("${air-quality.waqi.token}")
    private String token;
    
    @Value("${air-quality.openaq.radius-meters:500}")
    private int defaultRadiusMeters;
    
    @Value("${air-quality.openaq.max-hotspots:3}")
    private int maxHotspots;

    public OpenAQService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get the most polluted locations near the given coordinates
     * Using WAQI geo-location API to find nearby stations
     * 
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radiusMeters Search radius in meters (optional, uses default if null)
     * @param limit Maximum number of hotspots to return (optional, uses default if null)
     * @return Response containing the most polluted locations
     */
    public PollutionHotspotsResponseDTO getPollutionHotspots(
            Double latitude, 
            Double longitude, 
            Integer radiusMeters,
            Integer limit) {
        
        int radius = radiusMeters != null ? radiusMeters : defaultRadiusMeters;
        int maxResults = limit != null ? limit : maxHotspots;
        
        try {
            // WAQI map/bounds API: returns all stations in a bounding box
            // Calculate bounding box from center point and radius
            double latDelta = (radius / 1000.0) / 111.0; // 1 degree lat ≈ 111km
            double lonDelta = (radius / 1000.0) / (111.0 * Math.cos(Math.toRadians(latitude)));
            
            double lat1 = latitude - latDelta;
            double lon1 = longitude - lonDelta;
            double lat2 = latitude + latDelta;
            double lon2 = longitude + lonDelta;
            
            String url = String.format(
                "%s/map/bounds/?latlng=%f,%f,%f,%f&token=%s",
                baseUrl, lat1, lon1, lat2, lon2, token
            );
            
            log.info("Fetching pollution data from WAQI: lat={}, lon={}, radius={}m", 
                    latitude, longitude, radius);
            
            String response = null;
            try {
                response = restTemplate.getForObject(url, String.class);
            } catch (Exception e) {
                log.warn("Error fetching WAQI data, using simulated data: {}", e.getMessage());
                return getSimulatedHotspots(latitude, longitude, radius, maxResults);
            }
            
            JsonNode root = objectMapper.readTree(response);
            
            if (root == null || !root.has("data") || root.get("status").asText().equals("error")) {
                log.warn("Invalid WAQI response, using simulated data");
                return getSimulatedHotspots(latitude, longitude, radius, maxResults);
            }
            
            if (!root.get("data").isArray()) {
                log.warn("No pollution data found near coordinates ({}, {}), using simulated data", latitude, longitude);
                return getSimulatedHotspots(latitude, longitude, radius, maxResults);
            }
            
            List<PollutionHotspotDTO> hotspots = new ArrayList<>();
            JsonNode data = root.get("data");
            
            for (JsonNode station : data) {
                try {
                    if (!station.has("lat") || !station.has("lon") || !station.has("aqi")) {
                        continue;
                    }
                    
                    double stationLat = station.get("lat").asDouble();
                    double stationLon = station.get("lon").asDouble();
                    int aqi = station.get("aqi").asInt();
                    
                    // Calculate distance to filter by radius
                    double distance = calculateDistance(latitude, longitude, stationLat, stationLon);
                    if (distance > radius) {
                        continue; // Skip stations outside radius
                    }
                    
                    String stationName = station.has("station") && station.get("station").has("name") 
                            ? station.get("station").get("name").asText() 
                            : "Unknown Station";
                    
                    String aqiStatus = getAQIStatus(aqi);
                    String aqiColor = AQIColorUtil.getAQIColor(aqi);
                    
                    // Estimate PM2.5 from AQI (reverse calculation)
                    Double pm25 = estimatePM25FromAQI(aqi);
                    
                    String uid = station.has("uid") ? station.get("uid").asText() : "";
                    
                    PollutionHotspotDTO hotspot = new PollutionHotspotDTO(
                            stationName,
                            stationLat,
                            stationLon,
                            pm25,
                            "µg/m³",
                            aqi,
                            aqiStatus,
                            aqiColor,
                            new Date().toString()
                    );
                    
                    hotspots.add(hotspot);
                } catch (Exception e) {
                    log.warn("Error parsing station data: {}", e.getMessage());
                }
            }
            
            // If no real data, fallback to simulated
            if (hotspots.isEmpty()) {
                log.info("No real stations found, using simulated data");
                return getSimulatedHotspots(latitude, longitude, radius, maxResults);
            }
            
            // Sort by AQI descending and limit results
            List<PollutionHotspotDTO> topHotspots = hotspots.stream()
                    .sorted((a, b) -> Integer.compare(b.aqi(), a.aqi()))
                    .limit(maxResults)
                    .collect(Collectors.toList());
            
            String message = topHotspots.isEmpty() 
                    ? "No valid AQI measurements found in this area"
                    : String.format("Found %d pollution hotspots within %dm", topHotspots.size(), radius);
            
            log.info("Found {} pollution hotspots near ({}, {})", topHotspots.size(), latitude, longitude);
            
            return new PollutionHotspotsResponseDTO(
                    latitude, longitude, radius, hotspots.size(), topHotspots, message
            );
            
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching WAQI data: {}", e.getMessage());
            return createErrorResponse(latitude, longitude, radius, 
                    "Error fetching pollution data: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Network error fetching WAQI data: {}", e.getMessage());
            return createErrorResponse(latitude, longitude, radius, 
                    "Network error: Unable to reach WAQI service");
        } catch (Exception e) {
            log.error("Unexpected error fetching WAQI data", e);
            return createErrorResponse(latitude, longitude, radius, 
                    "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Calculate distance between two points in meters using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Estimate PM2.5 value from AQI (reverse calculation)
     */
    private Double estimatePM25FromAQI(Integer aqi) {
        if (aqi == null || aqi < 0) return 0.0;
        
        // Reverse EPA AQI breakpoints
        if (aqi <= 50) return reverseLinearInterpolation(aqi, 0, 50, 0.0, 12.0);
        if (aqi <= 100) return reverseLinearInterpolation(aqi, 51, 100, 12.1, 35.4);
        if (aqi <= 150) return reverseLinearInterpolation(aqi, 101, 150, 35.5, 55.4);
        if (aqi <= 200) return reverseLinearInterpolation(aqi, 151, 200, 55.5, 150.4);
        if (aqi <= 300) return reverseLinearInterpolation(aqi, 201, 300, 150.5, 250.4);
        if (aqi <= 400) return reverseLinearInterpolation(aqi, 301, 400, 250.5, 350.4);
        if (aqi <= 500) return reverseLinearInterpolation(aqi, 401, 500, 350.5, 500.4);
        
        return 500.0;
    }
    
    private Double reverseLinearInterpolation(Integer aqi, Integer lowAQI, Integer highAQI,
                                              Double lowConc, Double highConc) {
        return ((aqi - lowAQI) / (double)(highAQI - lowAQI)) * (highConc - lowConc) + lowConc;
    }
    
    /**
     * Calculate AQI from PM2.5 concentration (µg/m³)
     * Using EPA's AQI calculation formula
     */
    private Integer calculateAQIFromPM25(Double pm25) {
        if (pm25 < 0) return 0;
        
        // EPA AQI breakpoints for PM2.5 (24-hour average)
        if (pm25 <= 12.0) return linearInterpolation(pm25, 0.0, 12.0, 0, 50);
        if (pm25 <= 35.4) return linearInterpolation(pm25, 12.1, 35.4, 51, 100);
        if (pm25 <= 55.4) return linearInterpolation(pm25, 35.5, 55.4, 101, 150);
        if (pm25 <= 150.4) return linearInterpolation(pm25, 55.5, 150.4, 151, 200);
        if (pm25 <= 250.4) return linearInterpolation(pm25, 150.5, 250.4, 201, 300);
        if (pm25 <= 350.4) return linearInterpolation(pm25, 250.5, 350.4, 301, 400);
        if (pm25 <= 500.4) return linearInterpolation(pm25, 350.5, 500.4, 401, 500);
        
        return 500; // Max AQI
    }
    
    private Integer linearInterpolation(Double value, Double lowConc, Double highConc, 
                                       Integer lowAQI, Integer highAQI) {
        return (int) Math.round(
            ((value - lowConc) / (highConc - lowConc)) * (highAQI - lowAQI) + lowAQI
        );
    }
    
    private String getAQIStatus(Integer aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }
    
    /**
     * Generate simulated pollution hotspots near the given location
     * This is used when real API data is not available
     */
    private PollutionHotspotsResponseDTO getSimulatedHotspots(
            Double latitude, Double longitude, Integer radius, Integer limit) {
        
        log.info("Generating {} simulated hotspots near ({}, {}) within {}m", 
                limit, latitude, longitude, radius);
        
        List<PollutionHotspotDTO> simulatedHotspots = new ArrayList<>();
        Random random = new Random((long)(latitude * 1000000 + longitude * 1000000)); // Deterministic based on location
        
        // Generate 3-5 simulated stations
        int numStations = Math.min(limit + 1, 5);
        
        for (int i = 0; i < numStations; i++) {
            // Generate random offset within radius - distributed more widely across the city
            double angle = random.nextDouble() * 2 * Math.PI;
            // Use full radius range for better distribution across city
            double minDistance = radius * 0.3; // At least 30% away from center
            double maxDistance = radius * 0.95; // Up to 95% of radius
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
            
            double latOffset = (distance * Math.cos(angle)) / 111000.0; // meters to degrees
            double lonOffset = (distance * Math.sin(angle)) / (111000.0 * Math.cos(Math.toRadians(latitude)));
            
            double stationLat = latitude + latOffset;
            double stationLon = longitude + lonOffset;
            
            // Generate realistic AQI values (biased towards moderate pollution)
            int baseAQI = 60 + random.nextInt(90); // 60-150 range
            int aqi = baseAQI + (random.nextInt(3) - 1) * 20; // Add some variation
            aqi = Math.max(30, Math.min(200, aqi)); // Clamp between 30 and 200
            
            String[] stationTypes = {
                "Estación de Tráfico",
                "Estación Industrial",
                "Estación Urbana",
                "Zona Comercial",
                "Área Residencial"
            };
            
            String stationName = stationTypes[i % stationTypes.length] + " #" + (i + 1);
            
            Double pm25 = estimatePM25FromAQI(aqi);
            String aqiStatus = getAQIStatus(aqi);
            String aqiColor = AQIColorUtil.getAQIColor(aqi);
            
            PollutionHotspotDTO hotspot = new PollutionHotspotDTO(
                    stationName,
                    stationLat,
                    stationLon,
                    pm25,
                    "µg/m³",
                    aqi,
                    aqiStatus,
                    aqiColor,
                    new Date().toString()
            );
            
            simulatedHotspots.add(hotspot);
        }
        
        // Sort by AQI descending and limit
        List<PollutionHotspotDTO> topHotspots = simulatedHotspots.stream()
                .sorted((a, b) -> Integer.compare(b.aqi(), a.aqi()))
                .limit(limit)
                .collect(Collectors.toList());
        
        String message = String.format("Found %d pollution hotspots within %dm (simulated data)", 
                topHotspots.size(), radius);
        
        return new PollutionHotspotsResponseDTO(
                latitude, longitude, radius, simulatedHotspots.size(), topHotspots, message
        );
    }
    
    private PollutionHotspotsResponseDTO createErrorResponse(
            Double lat, Double lon, Integer radius, String errorMessage) {
        return new PollutionHotspotsResponseDTO(
                lat, lon, radius, 0, Collections.emptyList(), errorMessage
        );
    }
}

