package com.aether.app.infrastructure.web.controller;

import com.aether.app.infrastructure.web.dto.RouteGeoJsonDTO;
import com.aether.app.strava.StravaActivityService;
import com.aether.app.strava.StravaAuthService;
import com.aether.app.strava.StravaToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for StravaController routes endpoint
 */
@WebMvcTest(StravaController.class)
class StravaControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private StravaAuthService stravaAuthService;
    
    @MockBean
    private StravaActivityService stravaActivityService;
    
    @Test
    void testGetRoutesAsGeoJson_Success() throws Exception {
        // Setup mock data
        Long athleteId = 123456L;
        String city = "Madrid";
        
        StravaToken mockToken = createMockToken(athleteId);
        when(stravaAuthService.getValidToken(athleteId))
                .thenReturn(Optional.of(mockToken));
        
        List<StravaActivityService.RouteGroup> mockRoutes = createMockRouteGroups();
        when(stravaActivityService.getMostRepeatedRoutes(eq(athleteId), eq(city), anyInt()))
                .thenReturn(mockRoutes);
        
        // Perform request
        mockMvc.perform(get("/api/v1/strava/routes/geojson")
                        .param("athleteId", athleteId.toString())
                        .param("city", city)
                        .param("limit", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.metadata.athleteId").value(athleteId))
                .andExpect(jsonPath("$.metadata.city").value(city))
                .andExpect(jsonPath("$.metadata.totalRoutes").exists());
    }
    
    @Test
    void testGetRoutesAsGeoJson_NotAuthenticated() throws Exception {
        // Setup: User not authenticated
        Long athleteId = 123456L;
        when(stravaAuthService.getValidToken(athleteId))
                .thenReturn(Optional.empty());
        
        // Perform request
        mockMvc.perform(get("/api/v1/strava/routes/geojson")
                        .param("athleteId", athleteId.toString())
                        .param("city", "Madrid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("not_authenticated"));
    }
    
    @Test
    void testGetRoutesAsGeoJson_NoRoutesFound() throws Exception {
        // Setup mock data
        Long athleteId = 123456L;
        String city = "NonExistentCity";
        
        StravaToken mockToken = createMockToken(athleteId);
        when(stravaAuthService.getValidToken(athleteId))
                .thenReturn(Optional.of(mockToken));
        
        // Return empty list
        when(stravaActivityService.getMostRepeatedRoutes(eq(athleteId), eq(city), anyInt()))
                .thenReturn(List.of());
        
        // Perform request
        mockMvc.perform(get("/api/v1/strava/routes/geojson")
                        .param("athleteId", athleteId.toString())
                        .param("city", city)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features").isEmpty())
                .andExpect(jsonPath("$.metadata.totalRoutes").value(0))
                .andExpect(jsonPath("$.metadata.message").value("No routes found in this city"));
    }
    
    @Test
    void testGetRoutesAsGeoJson_MissingParameters() throws Exception {
        // Test without athleteId
        mockMvc.perform(get("/api/v1/strava/routes/geojson")
                        .param("city", "Madrid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        // Test without city
        mockMvc.perform(get("/api/v1/strava/routes/geojson")
                        .param("athleteId", "123456")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetRoutesAsGeoJson_DefaultLimit() throws Exception {
        // Test that limit defaults to 3 if not provided
        Long athleteId = 123456L;
        String city = "Madrid";
        
        StravaToken mockToken = createMockToken(athleteId);
        when(stravaAuthService.getValidToken(athleteId))
                .thenReturn(Optional.of(mockToken));
        
        when(stravaActivityService.getMostRepeatedRoutes(eq(athleteId), eq(city), eq(3)))
                .thenReturn(List.of());
        
        mockMvc.perform(get("/api/v1/strava/routes/geojson")
                        .param("athleteId", athleteId.toString())
                        .param("city", city)
                        // No limit parameter
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
    
    @Test
    void testGetRoutesAsGeoJson_CustomLimit() throws Exception {
        // Test with custom limit
        Long athleteId = 123456L;
        String city = "Madrid";
        int customLimit = 5;
        
        StravaToken mockToken = createMockToken(athleteId);
        when(stravaAuthService.getValidToken(athleteId))
                .thenReturn(Optional.of(mockToken));
        
        when(stravaActivityService.getMostRepeatedRoutes(eq(athleteId), eq(city), eq(customLimit)))
                .thenReturn(List.of());
        
        mockMvc.perform(get("/api/v1/strava/routes/geojson")
                        .param("athleteId", athleteId.toString())
                        .param("city", city)
                        .param("limit", String.valueOf(customLimit))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
    
    @Test
    void testGetRoutesAsGeoJson_GeoJsonStructure() throws Exception {
        // Test that the response has correct GeoJSON structure
        Long athleteId = 123456L;
        String city = "Madrid";
        
        StravaToken mockToken = createMockToken(athleteId);
        when(stravaAuthService.getValidToken(athleteId))
                .thenReturn(Optional.of(mockToken));
        
        List<StravaActivityService.RouteGroup> mockRoutes = createMockRouteGroups();
        when(stravaActivityService.getMostRepeatedRoutes(eq(athleteId), eq(city), anyInt()))
                .thenReturn(mockRoutes);
        
        mockMvc.perform(get("/api/v1/strava/routes/geojson")
                        .param("athleteId", athleteId.toString())
                        .param("city", city)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features[0].type").value("Feature"))
                .andExpect(jsonPath("$.features[0].geometry.type").value("LineString"))
                .andExpect(jsonPath("$.features[0].geometry.coordinates").isArray())
                .andExpect(jsonPath("$.features[0].properties.activityId").exists())
                .andExpect(jsonPath("$.features[0].properties.name").exists())
                .andExpect(jsonPath("$.features[0].properties.repetitions").exists())
                .andExpect(jsonPath("$.features[0].properties.color").exists());
    }
    
    // Helper methods
    
    private StravaToken createMockToken(Long athleteId) {
        StravaToken token = new StravaToken();
        token.setAthleteId(athleteId);
        token.setAccessToken("test_access_token");
        token.setRefreshToken("test_refresh_token");
        token.setTokenType("Bearer");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setFirstName("Test");
        token.setLastName("User");
        token.setUsername("testuser");
        return token;
    }
    
    private List<StravaActivityService.RouteGroup> createMockRouteGroups() {
        List<StravaActivityService.RouteGroup> groups = new ArrayList<>();
        
        // Create a route group with 3 repetitions
        StravaActivityService.RouteGroup group = new StravaActivityService.RouteGroup();
        group.representativeActivity = createMockActivity(1L, "Morning Run");
        group.activities = List.of(
                createMockActivity(1L, "Morning Run"),
                createMockActivity(2L, "Morning Run"),
                createMockActivity(3L, "Morning Run")
        );
        
        groups.add(group);
        return groups;
    }
    
    private com.aether.app.infrastructure.web.dto.StravaActivityDTO createMockActivity(
            Long id, String name) {
        
        // Simple polyline for testing (encodes a few points)
        String testPolyline = "_p~iF~ps|U_ulLnnqC";
        
        var map = new com.aether.app.infrastructure.web.dto.StravaActivityDTO.MapDTO(
                "map_" + id,
                testPolyline,
                null
        );
        
        return new com.aether.app.infrastructure.web.dto.StravaActivityDTO(
                id,
                name,
                "Run",
                "2025-10-04T10:00:00Z",
                "2025-10-04T10:00:00Z",
                5000.0,
                1800,
                2000,
                50.0,
                List.of(40.4168, -3.7038),
                List.of(40.4178, -3.7048),
                "Madrid",
                "Madrid",
                "Spain",
                map
        );
    }
}

