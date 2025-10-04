package com.aether.app.infrastructure.web.controller;

import com.aether.app.infrastructure.web.dto.StravaActivitiesResponseDTO;
import com.aether.app.infrastructure.web.dto.StravaActivityDTO;
import com.aether.app.infrastructure.web.dto.StravaAuthResponseDTO;
import com.aether.app.infrastructure.web.dto.StravaErrorDTO;
import com.aether.app.infrastructure.web.dto.StravaTokenResponseDTO;
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
    
    /**
     * Initiate OAuth flow - redirects user to Strava authorization page
     * 
     * GET /api/v1/strava/auth/login
     */
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
            // Exchange code for access token
            StravaTokenResponseDTO tokenResponse = stravaAuthService.exchangeCodeForToken(code);
            
            // Save token to database
            StravaToken savedToken = stravaAuthService.saveToken(tokenResponse);
            
            log.info("User authenticated successfully: {} (ID: {})", 
                    savedToken.getUsername(), savedToken.getAthleteId());
            
            // Redirect to main application page with success
            response.sendRedirect("/index.html?auth=success&athlete=" + savedToken.getAthleteId());
            
        } catch (Exception e) {
            log.error("Error during OAuth callback: {}", e.getMessage(), e);
            response.sendRedirect("/login.html?error=auth_failed");
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
     * Health check endpoint for Strava integration
     * 
     * GET /api/v1/strava/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Strava integration is operational");
    }
}

