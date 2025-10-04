package com.aether.app.infrastructure.web.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to decode Google Polyline format used by Strava
 * Based on Google's Polyline Encoding Algorithm
 */
public class PolylineUtil {
    
    /**
     * Decodes a Google Polyline string into a list of [longitude, latitude] pairs
     * @param encoded The encoded polyline string
     * @return List of coordinate pairs [lon, lat] (GeoJSON format)
     */
    public static List<List<Double>> decode(String encoded) {
        List<List<Double>> coords = new ArrayList<>();
        
        if (encoded == null || encoded.isEmpty()) {
            return coords;
        }
        
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;
        
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            
            // Decode latitude
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            
            shift = 0;
            result = 0;
            
            // Decode longitude
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            
            // Convert to decimal degrees and add to list
            // GeoJSON format is [longitude, latitude]
            double longitude = lng / 1E5;
            double latitude = lat / 1E5;
            coords.add(List.of(longitude, latitude));
        }
        
        return coords;
    }
    
    /**
     * Decodes polyline to [latitude, longitude] format (for backwards compatibility)
     * @param encoded The encoded polyline string
     * @return List of coordinate pairs [lat, lon]
     */
    public static List<List<Double>> decodeLatLon(String encoded) {
        List<List<Double>> coords = new ArrayList<>();
        
        if (encoded == null || encoded.isEmpty()) {
            return coords;
        }
        
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;
        
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            
            // Decode latitude
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            
            shift = 0;
            result = 0;
            
            // Decode longitude
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            
            // Convert to decimal degrees and add to list [lat, lon]
            double latitude = lat / 1E5;
            double longitude = lng / 1E5;
            coords.add(List.of(latitude, longitude));
        }
        
        return coords;
    }
}

