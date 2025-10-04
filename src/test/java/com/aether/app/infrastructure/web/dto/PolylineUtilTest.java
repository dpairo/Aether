package com.aether.app.infrastructure.web.dto;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolylineUtil
 * Tests the Google Polyline decoding algorithm
 */
class PolylineUtilTest {
    
    @Test
    void testDecodeSimplePolyline() {
        // Example polyline from Google Maps Polyline documentation
        // Represents a line from (38.5, -120.2) to (40.7, -120.95) to (43.252, -126.453)
        String encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
        
        List<List<Double>> coords = PolylineUtil.decode(encoded);
        
        assertNotNull(coords);
        assertFalse(coords.isEmpty());
        assertEquals(3, coords.size(), "Should decode to 3 points");
        
        // Verify first point (approximately)
        assertEquals(-120.2, coords.get(0).get(0), 0.1, "First longitude");
        assertEquals(38.5, coords.get(0).get(1), 0.1, "First latitude");
        
        // Verify second point (approximately)
        assertEquals(-120.95, coords.get(1).get(0), 0.1, "Second longitude");
        assertEquals(40.7, coords.get(1).get(1), 0.1, "Second latitude");
        
        // Verify third point (approximately)
        assertEquals(-126.453, coords.get(2).get(0), 0.1, "Third longitude");
        assertEquals(43.252, coords.get(2).get(1), 0.1, "Third latitude");
    }
    
    @Test
    void testDecodeEmptyString() {
        List<List<Double>> coords = PolylineUtil.decode("");
        
        assertNotNull(coords);
        assertTrue(coords.isEmpty(), "Empty string should return empty list");
    }
    
    @Test
    void testDecodeNull() {
        List<List<Double>> coords = PolylineUtil.decode(null);
        
        assertNotNull(coords);
        assertTrue(coords.isEmpty(), "Null should return empty list");
    }
    
    @Test
    void testDecodeMadridRoute() {
        // Polyline for a route in Madrid (sample from Strava)
        // This is a simplified polyline around Retiro Park area
        String encoded = "siq~Fj`jM??";
        
        List<List<Double>> coords = PolylineUtil.decode(encoded);
        
        assertNotNull(coords);
        assertFalse(coords.isEmpty());
        
        // Each coordinate should have exactly 2 elements (lon, lat)
        for (List<Double> coord : coords) {
            assertEquals(2, coord.size(), "Each coordinate should have lon and lat");
        }
    }
    
    @Test
    void testDecodeLatLonFormat() {
        // Test the alternative format that returns [lat, lon] instead of [lon, lat]
        String encoded = "_p~iF~ps|U_ulLnnqC";
        
        List<List<Double>> coordsLatLon = PolylineUtil.decodeLatLon(encoded);
        List<List<Double>> coordsLonLat = PolylineUtil.decode(encoded);
        
        assertNotNull(coordsLatLon);
        assertNotNull(coordsLonLat);
        assertEquals(coordsLatLon.size(), coordsLonLat.size());
        
        // Verify that lat/lon are swapped between the two methods
        if (!coordsLatLon.isEmpty() && !coordsLonLat.isEmpty()) {
            assertEquals(coordsLatLon.get(0).get(0), coordsLonLat.get(0).get(1), 0.00001);
            assertEquals(coordsLatLon.get(0).get(1), coordsLonLat.get(0).get(0), 0.00001);
        }
    }
    
    @Test
    void testDecodeRealStravaPolyline() {
        // Real Strava polyline sample (shortened for testing)
        // This represents a small segment of a real running route
        String encoded = "o{r~Fzclb@BGDIBEDGBG@G?G";
        
        List<List<Double>> coords = PolylineUtil.decode(encoded);
        
        assertNotNull(coords);
        assertTrue(coords.size() > 5, "Should decode multiple points");
        
        // Verify all coordinates are valid numbers
        for (List<Double> coord : coords) {
            assertFalse(coord.get(0).isNaN(), "Longitude should be valid");
            assertFalse(coord.get(1).isNaN(), "Latitude should be valid");
            assertTrue(coord.get(0) >= -180 && coord.get(0) <= 180, "Longitude in valid range");
            assertTrue(coord.get(1) >= -90 && coord.get(1) <= 90, "Latitude in valid range");
        }
    }
    
    @Test
    void testDecodePreservesOrder() {
        String encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
        
        List<List<Double>> coords = PolylineUtil.decode(encoded);
        
        // Points should be decoded in order
        assertTrue(coords.size() >= 2, "Should have at least 2 points");
        
        // The latitude should generally increase in this test polyline
        double firstLat = coords.get(0).get(1);
        double lastLat = coords.get(coords.size() - 1).get(1);
        
        assertTrue(lastLat > firstLat, "Route should progress northward in this test case");
    }
}

