package com.aether.app.strava;

import com.aether.app.infrastructure.web.dto.StravaActivityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to interact with Strava Activities API
 */
@Service
public class StravaActivityService {
    
    private static final Logger log = LoggerFactory.getLogger(StravaActivityService.class);
    private static final String STRAVA_API_BASE = "https://www.strava.com/api/v3";
    
    private final StravaAuthService authService;
    private final RestTemplate restTemplate;
    
    public StravaActivityService(StravaAuthService authService) {
        this.authService = authService;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Get athlete activities from Strava API
     */
    public List<StravaActivityDTO> getAthleteActivities(Long athleteId, int perPage, int page) {
        var tokenOpt = authService.getValidToken(athleteId);
        
        if (tokenOpt.isEmpty()) {
            log.warn("No valid token found for athlete: {}", athleteId);
            return List.of();
        }
        
        String accessToken = tokenOpt.get().getAccessToken();
        
        // Build request with authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        String url = String.format("%s/athlete/activities?per_page=%d&page=%d", 
                STRAVA_API_BASE, perPage, page);
        
        try {
            ResponseEntity<List<StravaActivityDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<StravaActivityDTO>>() {}
            );
            
            List<StravaActivityDTO> activities = response.getBody();
            log.info("Retrieved {} activities for athlete {}", 
                    activities != null ? activities.size() : 0, athleteId);
            
            return activities != null ? activities : List.of();
            
        } catch (Exception e) {
            log.error("Error fetching activities for athlete {}: {}", athleteId, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Get activities filtered by city name
     */
    public List<StravaActivityDTO> getActivitiesByCity(Long athleteId, String cityName) {
        // Get last 50 activities (can be adjusted)
        List<StravaActivityDTO> activities = getAthleteActivities(athleteId, 50, 1);
        
        if (activities.isEmpty()) {
            log.info("No activities found for athlete: {}", athleteId);
            return List.of();
        }
        
        // Filter by city (case insensitive)
        String searchCity = cityName.toLowerCase().trim();
        List<StravaActivityDTO> filtered = activities.stream()
                .filter(activity -> activity.locationCity() != null && 
                        activity.locationCity().toLowerCase().contains(searchCity))
                .collect(Collectors.toList());
        
        log.info("Found {} activities in city '{}' for athlete {}", 
                filtered.size(), cityName, athleteId);
        
        return filtered;
    }
    
    /**
     * Get activities near coordinates (simple distance check)
     */
    public List<StravaActivityDTO> getActivitiesNearLocation(Long athleteId, double lat, double lon, double radiusKm) {
        List<StravaActivityDTO> activities = getAthleteActivities(athleteId, 50, 1);
        
        if (activities.isEmpty()) {
            return List.of();
        }
        
        // Filter activities that start within radius
        List<StravaActivityDTO> filtered = activities.stream()
                .filter(activity -> {
                    if (activity.startLatLng() == null || activity.startLatLng().size() < 2) {
                        return false;
                    }
                    
                    double actLat = activity.startLatLng().get(0);
                    double actLon = activity.startLatLng().get(1);
                    
                    double distance = calculateDistance(lat, lon, actLat, actLon);
                    return distance <= radiusKm;
                })
                .collect(Collectors.toList());
        
        log.info("Found {} activities within {}km of ({}, {}) for athlete {}", 
                filtered.size(), radiusKm, lat, lon, athleteId);
        
        return filtered;
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     * Returns distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
}

