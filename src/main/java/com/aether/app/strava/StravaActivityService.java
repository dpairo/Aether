package com.aether.app.strava;

import com.aether.app.infrastructure.web.dto.StravaActivityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
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
    
    /**
     * Get the most repeated routes in a city
     * Routes are grouped by similarity (start/end points within 100m)
     * Returns up to 3 most repeated routes with their activity list
     */
    public List<RouteGroup> getMostRepeatedRoutes(Long athleteId, String cityName, int maxRoutes) {
        List<StravaActivityDTO> activities = getActivitiesByCity(athleteId, cityName);
        
        if (activities.isEmpty()) {
            log.info("No activities found in city '{}' for athlete {}", cityName, athleteId);
            return List.of();
        }
        
        // Filter activities that have valid start/end coordinates and polyline
        List<StravaActivityDTO> validActivities = activities.stream()
                .filter(a -> a.startLatLng() != null && a.startLatLng().size() >= 2)
                .filter(a -> a.endLatLng() != null && a.endLatLng().size() >= 2)
                .filter(a -> a.map() != null && a.map().summaryPolyline() != null)
                .collect(Collectors.toList());
        
        if (validActivities.isEmpty()) {
            log.info("No valid activities with routes in city '{}'", cityName);
            return List.of();
        }
        
        // Group similar routes
        List<RouteGroup> routeGroups = new ArrayList<>();
        
        for (StravaActivityDTO activity : validActivities) {
            boolean addedToGroup = false;
            
            // Try to find a matching group
            for (RouteGroup group : routeGroups) {
                if (areRouteSimilar(activity, group.representativeActivity)) {
                    group.activities.add(activity);
                    addedToGroup = true;
                    break;
                }
            }
            
            // Create new group if no match found
            if (!addedToGroup) {
                RouteGroup newGroup = new RouteGroup();
                newGroup.representativeActivity = activity;
                newGroup.activities = new ArrayList<>();
                newGroup.activities.add(activity);
                routeGroups.add(newGroup);
            }
        }
        
        // Sort by number of repetitions (descending)
        routeGroups.sort((g1, g2) -> Integer.compare(g2.activities.size(), g1.activities.size()));
        
        // Return top N routes
        List<RouteGroup> topRoutes = routeGroups.stream()
                .limit(maxRoutes)
                .collect(Collectors.toList());
        
        log.info("Found {} unique routes in city '{}', returning top {}", 
                routeGroups.size(), cityName, topRoutes.size());
        
        return topRoutes;
    }
    
    /**
     * Check if two routes are similar based on start and end points
     * Routes are considered similar if start and end points are within 100m
     */
    private boolean areRouteSimilar(StravaActivityDTO route1, StravaActivityDTO route2) {
        final double SIMILARITY_THRESHOLD_KM = 0.1; // 100 meters
        
        double startLat1 = route1.startLatLng().get(0);
        double startLon1 = route1.startLatLng().get(1);
        double endLat1 = route1.endLatLng().get(0);
        double endLon1 = route1.endLatLng().get(1);
        
        double startLat2 = route2.startLatLng().get(0);
        double startLon2 = route2.startLatLng().get(1);
        double endLat2 = route2.endLatLng().get(0);
        double endLon2 = route2.endLatLng().get(1);
        
        double startDistance = calculateDistance(startLat1, startLon1, startLat2, startLon2);
        double endDistance = calculateDistance(endLat1, endLon1, endLat2, endLon2);
        
        return startDistance <= SIMILARITY_THRESHOLD_KM && endDistance <= SIMILARITY_THRESHOLD_KM;
    }
    
    /**
     * Helper class to group similar routes
     */
    public static class RouteGroup {
        public StravaActivityDTO representativeActivity;
        public List<StravaActivityDTO> activities;
        
        public int getRepetitions() {
            return activities.size();
        }
    }
}


