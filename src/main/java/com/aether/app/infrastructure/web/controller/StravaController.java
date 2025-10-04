package com.aether.app.infrastructure.web.controller;

import com.aether.app.infrastructure.web.dto.*;
import com.aether.app.strava.StravaActivityService;
import com.aether.app.strava.StravaAuthService;
import com.aether.app.strava.StravaToken;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Strava OAuth2 authentication and activities
 */
@RestController
@RequestMapping("/api/v1/strava")
public class StravaController {
    
    private static final Logger log = LoggerFactory.getLogger(StravaController.class);
    
    private final StravaAuthService stravaAuthService;
    private final StravaActivityService stravaActivityService;
    
    public StravaController(StravaAuthService stravaAuthService, StravaActivityService stravaActivityService) {
        this.stravaAuthService = stravaAuthService;
        this.stravaActivityService = stravaActivityService;
    }
    
    @GetMapping("/auth/login")
    public void login(HttpServletResponse response) throws IOException {
        // Generate random state for CSRF protection
        String state = UUID.randomUUID().toString();
        
        // In a production environment, you should store the state in session
        // or database to validate it in the callback
        
        String authUrl = stravaAuthService.getAuthorizationUrl(state);
        log.info("Redirecting to Strava authorization: {}", authUrl);
        
        response.sendRedirect(authUrl);
    }
    
    /**
     * OAuth callback endpoint - Strava redirects here after user authorization
     * 
     * GET /api/v1/strava/auth/callback?code=xxx&state=xxx
     */
    @GetMapping("/auth/callback")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response
    ) throws IOException {
        
        // Handle authorization denial
        if (error != null) {
            log.warn("Strava authorization denied: {}", error);
            response.sendRedirect("/login.html?error=access_denied");
            return;
        }
        
        // Validate required parameters
        if (code == null || code.isBlank()) {
            log.error("Missing authorization code in callback");
            response.sendRedirect("/login.html?error=missing_code");
            return;
        }
        
        try {
            log.info("Attempting to exchange code for token...");
            
            // Exchange code for access token
            StravaTokenResponseDTO tokenResponse = stravaAuthService.exchangeCodeForToken(code);
            
            log.info("Token exchange successful. Saving token...");
            
            // Save token to database
            StravaToken savedToken = stravaAuthService.saveToken(tokenResponse);
            
            log.info("User authenticated successfully: {} (ID: {})", 
                    savedToken.getUsername(), savedToken.getAthleteId());
            
            // Redirect to main application page with success
            response.sendRedirect("/index.html?auth=success&athlete=" + savedToken.getAthleteId());
            
        } catch (Exception e) {
            log.error("❌ Error during OAuth callback: {}", e.getMessage(), e);
            log.error("❌ Full stack trace:", e);
            
            // Send more detailed error message
            String errorMsg = e.getMessage() != null ? e.getMessage() : "unknown_error";
            response.sendRedirect("/login.html?error=auth_failed&details=" + 
                    java.net.URLEncoder.encode(errorMsg, java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    /**
     * Get current authenticated user information
     * 
     * GET /api/v1/strava/auth/me?athleteId=xxx
     */
    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser(@RequestParam Long athleteId) {
        try {
            var tokenOpt = stravaAuthService.getValidToken(athleteId);
            
            if (tokenOpt.isPresent()) {
                var token = tokenOpt.get();
                return ResponseEntity.ok(new StravaAuthResponseDTO(
                        token.getAthleteId(),
                        token.getFirstName(),
                        token.getLastName(),
                        token.getUsername(),
                        token.getAccessToken(),
                        token.getExpiresAt().getEpochSecond(),
                        "Authenticated successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new StravaErrorDTO("not_authenticated", "User not authenticated"));
            }
        } catch (Exception e) {
            log.error("Error retrieving user info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StravaErrorDTO("internal_error", "Failed to retrieve user information"));
        }
    }
    
    /**
     * Logout - revoke Strava token
     * 
     * POST /api/v1/strava/auth/logout?athleteId=xxx
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@RequestParam Long athleteId) {
        try {
            stravaAuthService.revokeToken(athleteId);
            log.info("User logged out: {}", athleteId);
            return ResponseEntity.ok().body(new StravaAuthResponseDTO(
                    athleteId, null, null, null, null, null, "Logged out successfully"
            ));
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StravaErrorDTO("internal_error", "Failed to logout"));
        }
    }
    
    /**
     * Get activities in a specific city
     * 
     * GET /api/v1/strava/activities/city?athleteId=xxx&city=Madrid
     */
    @GetMapping("/activities/city")
    public ResponseEntity<?> getActivitiesByCity(
            @RequestParam Long athleteId,
            @RequestParam String city
    ) {
        try {
            log.info("Searching activities for athlete {} in city: {}", athleteId, city);
            
            List<StravaActivityDTO> activities = stravaActivityService.getActivitiesByCity(athleteId, city);
            
            if (activities.isEmpty()) {
                log.info("No activities found in city '{}' for athlete {}", city, athleteId);
                return ResponseEntity.ok(new StravaActivitiesResponseDTO(
                        athleteId, city, null, null, 0, List.of(), 
                        "No activities found in this city"
                ));
            }
            
            // Get location info from first activity if available
            String state = activities.get(0).locationState();
            String country = activities.get(0).locationCountry();
            
            return ResponseEntity.ok(new StravaActivitiesResponseDTO(
                    athleteId, city, state, country, activities.size(), activities,
                    "Found " + activities.size() + " activities in " + city
            ));
            
        } catch (Exception e) {
            log.error("Error fetching activities by city: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StravaErrorDTO("internal_error", "Failed to fetch activities"));
        }
    }
    
    /**
     * Get activities near a location
     * 
     * GET /api/v1/strava/activities/near?athleteId=xxx&lat=40.4168&lon=-3.7038&radius=10
     */
    @GetMapping("/activities/near")
    public ResponseEntity<?> getActivitiesNearLocation(
            @RequestParam Long athleteId,
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(defaultValue = "10") Double radius
    ) {
        try {
            log.info("Searching activities for athlete {} near ({}, {}) within {}km", 
                    athleteId, lat, lon, radius);
            
            List<StravaActivityDTO> activities = stravaActivityService
                    .getActivitiesNearLocation(athleteId, lat, lon, radius);
            
            if (activities.isEmpty()) {
                log.info("No activities found near location for athlete {}", athleteId);
                return ResponseEntity.ok(new StravaActivitiesResponseDTO(
                        athleteId, null, null, null, 0, List.of(),
                        "No activities found near this location"
                ));
            }
            
            return ResponseEntity.ok(new StravaActivitiesResponseDTO(
                    athleteId, null, null, null, activities.size(), activities,
                    "Found " + activities.size() + " activities near location"
            ));
            
        } catch (Exception e) {
            log.error("Error fetching activities near location: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StravaErrorDTO("internal_error", "Failed to fetch activities"));
        }
    }
    
    /**
     * Get most repeated routes in a city as GeoJSON
     * This endpoint is called when user has both location and Strava authentication
     * 
     * GET /api/v1/strava/routes/geojson?athleteId=xxx&city=Madrid&limit=3
     */
    @GetMapping("/routes/geojson")
    public ResponseEntity<?> getRoutesAsGeoJson(
            @RequestParam Long athleteId,
            @RequestParam String city,
            @RequestParam(defaultValue = "3") Integer limit
    ) {
        try {
            log.info("Fetching top {} routes for athlete {} in city '{}'", limit, athleteId, city);
            
            // Check if user is authenticated
            var tokenOpt = stravaAuthService.getValidToken(athleteId);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new StravaErrorDTO("not_authenticated", "User not authenticated with Strava"));
            }
            
            // Get most repeated routes
            List<StravaActivityService.RouteGroup> routeGroups = 
                    stravaActivityService.getMostRepeatedRoutes(athleteId, city, limit);
            
            if (routeGroups.isEmpty()) {
                log.info("No routes found in city '{}' for athlete {}", city, athleteId);
                
                RouteGeoJsonDTO.RouteMetadata emptyMetadata = new RouteGeoJsonDTO.RouteMetadata(
                        athleteId, city, 0, 0, "No routes found in this city"
                );
                
                return ResponseEntity.ok(RouteGeoJsonDTO.create(List.of(), emptyMetadata));
            }
            
            // Convert to GeoJSON features
            List<RouteGeoJsonDTO.RouteFeature> features = new ArrayList<>();
            int totalRepetitions = 0;
            
            // Define colors for different routes
            String[] colors = {"#E74C3C", "#3498DB", "#2ECC71"};
            
            for (int i = 0; i < routeGroups.size(); i++) {
                StravaActivityService.RouteGroup group = routeGroups.get(i);
                StravaActivityDTO representative = group.representativeActivity;
                int repetitions = group.getRepetitions();
                totalRepetitions += repetitions;
                
                // Decode polyline to coordinates
                String polyline = representative.map().summaryPolyline();
                if (polyline == null || polyline.isEmpty()) {
                    log.warn("Activity {} has no polyline, skipping", representative.id());
                    continue;
                }
                
                List<List<Double>> coordinates = PolylineUtil.decode(polyline);
                
                if (coordinates.isEmpty()) {
                    log.warn("Failed to decode polyline for activity {}", representative.id());
                    continue;
                }
                
                // Create properties
                RouteGeoJsonDTO.RouteProperties properties = new RouteGeoJsonDTO.RouteProperties(
                        representative.id(),
                        representative.name(),
                        representative.type(),
                        representative.distance(),
                        representative.movingTime(),
                        representative.startDateLocal(),
                        repetitions,
                        colors[i % colors.length],
                        representative.locationCity()
                );
                
                // Create feature
                features.add(RouteGeoJsonDTO.RouteFeature.create(coordinates, properties));
            }
            
            // Create metadata
            RouteGeoJsonDTO.RouteMetadata metadata = new RouteGeoJsonDTO.RouteMetadata(
                    athleteId,
                    city,
                    features.size(),
                    totalRepetitions,
                    String.format("Found %d unique routes with %d total activities", 
                            features.size(), totalRepetitions)
            );
            
            // Create GeoJSON response
            RouteGeoJsonDTO geoJson = RouteGeoJsonDTO.create(features, metadata);
            
            log.info("Returning {} routes as GeoJSON for athlete {} in city '{}'", 
                    features.size(), athleteId, city);
            
            return ResponseEntity.ok(geoJson);
            
        } catch (Exception e) {
            log.error("Error fetching routes as GeoJSON: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StravaErrorDTO("internal_error", "Failed to fetch routes: " + e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint for Strava integration
     * 
     * GET /api/v1/strava/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Strava integration is operational");
    }
}

