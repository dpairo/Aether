package com.aether.app.air;

import com.aether.app.infrastructure.web.dto.CitySearchDTO;
import com.aether.app.infrastructure.web.dto.NominatimResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service for interacting with the Nominatim (OpenStreetMap) API
 * to search for Spanish cities and retrieve their coordinates.
 */
@Service
public class NominatimService {

    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private final RestTemplate restTemplate;

    public NominatimService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Search for a Spanish city by name using Nominatim API
     * 
     * @param cityName The name of the city to search for
     * @return CitySearchDTO with city coordinates and details, or null if not found
     */
    public CitySearchDTO searchSpanishCity(String cityName) {
        try {
            // Build the Nominatim API URL with proper parameters
            String url = UriComponentsBuilder.fromHttpUrl(NOMINATIM_BASE_URL)
                    .queryParam("format", "json")
                    .queryParam("q", cityName + ", España")
                    .queryParam("countrycodes", "es")
                    .queryParam("limit", "1")
                    .queryParam("addressdetails", "1")
                    .queryParam("accept-language", "es")
                    .build()
                    .toUriString();

            // Make the request
            NominatimResponseDTO[] responses = restTemplate.getForObject(url, NominatimResponseDTO[].class);

            // Check if we got results
            if (responses == null || responses.length == 0) {
                System.out.println("No se encontró la ciudad: " + cityName);
                return null;
            }

            // Get the first (best) result
            NominatimResponseDTO result = responses[0];

            // Extract city name from address
            String extractedCity = extractCityName(result);
            
            // Debug logging
            System.out.println("🔍 Nominatim search for: " + cityName);
            System.out.println("   Raw name: " + result.name());
            System.out.println("   Extracted city: " + extractedCity);
            if (result.address() != null) {
                System.out.println("   Address.city: " + result.address().city());
                System.out.println("   Address.town: " + result.address().town());
                System.out.println("   Address.municipality: " + result.address().municipality());
            }

            // Build and return the response DTO
            return new CitySearchDTO(
                    extractedCity,
                    result.lat(),
                    result.lon(),
                    result.displayName(),
                    result.boundingbox()
            );

        } catch (Exception e) {
            System.err.println("Error buscando ciudad en Nominatim: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract the city name from the Nominatim address details
     */
    private String extractCityName(NominatimResponseDTO response) {
        if (response.address() == null) {
            return cleanCityName(response.name());
        }

        // Try to get city name from address details (priority order)
        String city = response.address().city();
        if (city != null && !city.isEmpty()) {
            return cleanCityName(city);
        }

        city = response.address().town();
        if (city != null && !city.isEmpty()) {
            return cleanCityName(city);
        }

        city = response.address().village();
        if (city != null && !city.isEmpty()) {
            return cleanCityName(city);
        }

        city = response.address().municipality();
        if (city != null && !city.isEmpty()) {
            return cleanCityName(city);
        }

        // Fallback to the name field
        return cleanCityName(response.name());
    }

    /**
     * Clean city name by removing numbers and extra characters
     */
    private String cleanCityName(String cityName) {
        if (cityName == null) {
            return "";
        }
        
        // Remove any trailing numbers (e.g., "Valencia1" -> "Valencia")
        cityName = cityName.replaceAll("\\d+$", "");
        
        // Remove any extra whitespace
        cityName = cityName.trim();
        
        return cityName;
    }
}

