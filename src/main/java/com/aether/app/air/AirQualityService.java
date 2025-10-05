package com.aether.app.air;

import com.aether.app.infrastructure.web.dto.BestTimeResponseDTO;
import com.aether.app.infrastructure.web.dto.BestTimeSlotDTO;
import com.aether.app.infrastructure.web.dto.CityAirQualityDTO;
import com.aether.app.infrastructure.web.dto.WAQIResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;

@Service
public class AirQualityService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${air-quality.waqi.base-url}")
    private String baseUrl;
    
    @Value("${air-quality.waqi.token}")
    private String token;
    
    @Value("${air-quality.waqi.timeout:5000}")
    private int timeout;

    public AirQualityService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public CityAirQualityDTO getAirQualityByCity(String cityId) {
        try {
            String url = String.format("%s/feed/%s/?token=%s", baseUrl, cityId, token);
            WAQIResponseDTO response = restTemplate.getForObject(url, WAQIResponseDTO.class);
            
            if (response == null || response.data() == null) {
                // Return default response instead of throwing exception
                CityConfig cityConfig = getCityConfig(cityId);
                return createDefaultAirQuality(cityConfig);
            }
            
            // Get city coordinates from configuration
            CityConfig cityConfig = getCityConfig(cityId);
            return CityAirQualityDTO.fromWAQIResponse(
                cityConfig.name(),
                cityId,
                cityConfig.latitude(),
                cityConfig.longitude(),
                response
            );
        } catch (HttpClientErrorException e) {
            // Return default response instead of throwing exception
            CityConfig cityConfig = getCityConfig(cityId);
            return createDefaultAirQuality(cityConfig);
        } catch (ResourceAccessException e) {
            // Return default response instead of throwing exception
            CityConfig cityConfig = getCityConfig(cityId);
            return createDefaultAirQuality(cityConfig);
        } catch (Exception e) {
            // Return default response instead of throwing exception
            CityConfig cityConfig = getCityConfig(cityId);
            return createDefaultAirQuality(cityConfig);
        }
    }

    private CityAirQualityDTO createDefaultAirQuality(CityConfig city) {
        // Simular valores AQI para demostración
        int simulatedAqi = getSimulatedAQI(city.cityId());
        String status = getAQIStatus(simulatedAqi);
        
        return new CityAirQualityDTO(
                city.name(),
                city.cityId(),
                city.latitude(),
                city.longitude(),
                simulatedAqi,
                status,
                com.aether.app.infrastructure.web.dto.AQIColorUtil.getAQIColor(simulatedAqi),
                "pm25",
                new com.aether.app.infrastructure.web.dto.AirQualityDataDTO(
                        25.0, 35.0, 15.0, 45.0, 0.5, 5.0
                ),
                new Date().toString()
        );
    }
    
    private int getSimulatedAQI(String cityId) {
        // Valores simulados para demostración
        switch(cityId) {
            case "madrid": return 75;  // Moderado (amarillo)
            case "barcelona": return 45;  // Bueno (verde)
            case "valencia": return 55;  // Moderado (amarillo)
            case "alicante": return 85;  // Moderado (amarillo)
            case "sevilla": return 125;  // Dañino para sensibles (naranja)
            case "malaga": return 165;  // Dañino para todos (rojo)
            case "cordoba": return 145;  // Dañino para sensibles (naranja)
            case "bilbao": return 35;  // Bueno (verde)
            case "zaragoza": return 95;  // Moderado (amarillo)
            case "murcia": return 110;  // Dañino para sensibles (naranja)
            case "palma": return 25;  // Bueno (verde)
            case "las-palmas": return 15;  // Bueno (verde)
            case "valladolid": return 65;  // Moderado (amarillo)
            case "vigo": return 20;  // Bueno (verde)
            case "gijon": return 30;  // Bueno (verde)
            case "santander": return 28;  // Bueno (verde)
            case "toledo": return 90;  // Moderado (amarillo)
            case "badajoz": return 105;  // Dañino para sensibles (naranja)
            case "pamplona": return 40;  // Bueno (verde)
            case "logrono": return 60;  // Moderado (amarillo)
            default: return 50;
        }
    }
    
    private static String getAQIStatus(Integer aqi) {
        if (aqi == null) return "Unknown";
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    private CityConfig getCityConfig(String cityId) {
        return getSpanishCities().stream()
                .filter(city -> city.cityId().equals(cityId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("City not found: " + cityId));
    }

    private List<CityConfig> getSpanishCities() {
        return List.of(
                new CityConfig("Madrid", "madrid", 40.4168, -3.7038),
                new CityConfig("Barcelona", "barcelona", 41.3851, 2.1734),
                new CityConfig("Valencia", "valencia", 39.4699, -0.3763),
                new CityConfig("Sevilla", "sevilla", 37.3891, -5.9845),
                new CityConfig("Bilbao", "bilbao", 43.2627, -2.9253),
                new CityConfig("Zaragoza", "zaragoza", 41.6488, -0.8891),
                new CityConfig("Málaga", "malaga", 36.7213, -4.4214),
                new CityConfig("Murcia", "murcia", 37.9922, -1.1307),
                new CityConfig("Palma", "palma", 39.5696, 2.6502),
                new CityConfig("Las Palmas", "las-palmas", 28.1248, -15.4300),
                new CityConfig("Bilbao", "bilbao", 43.2627, -2.9253),
                new CityConfig("Alicante", "alicante", 38.3452, -0.4810),
                new CityConfig("Córdoba", "cordoba", 37.8847, -4.7791),
                new CityConfig("Valladolid", "valladolid", 41.6523, -4.7245),
                new CityConfig("Vigo", "vigo", 42.2406, -8.7207),
                new CityConfig("Gijón", "gijon", 43.5453, -5.6619),
                new CityConfig("L'Hospitalet", "hospitalet", 41.3596, 2.1004),
                new CityConfig("Granada", "granada", 37.1773, -3.5986),
                new CityConfig("San Sebastián", "donostia", 43.3183, -1.9812)
        );
    }

    private record CityConfig(String name, String cityId, Double latitude, Double longitude) {}
    
    /**
     * Get best time slots to go out based on predicted AQI
     */
    public BestTimeResponseDTO getBestTimeToGoOut(String cityId) {
        CityConfig cityConfig = getCityConfig(cityId);
        int baseAqi = getSimulatedAQI(cityId);
        
        List<BestTimeSlotDTO> timeSlots = new ArrayList<>();
        
        // Generate time slots with predicted AQI (simulated)
        // Early morning (6-8 AM) - usually best air quality
        timeSlots.add(createTimeSlot("06:00 - 08:00", baseAqi - 15));
        
        // Morning (8-10 AM) - air quality starts to decline
        timeSlots.add(createTimeSlot("08:00 - 10:00", baseAqi - 10));
        
        // Late morning (10-12 AM)
        timeSlots.add(createTimeSlot("10:00 - 12:00", baseAqi - 5));
        
        // Noon (12-14 PM) - moderate
        timeSlots.add(createTimeSlot("12:00 - 14:00", baseAqi));
        
        // Afternoon (14-16 PM) - usually worse
        timeSlots.add(createTimeSlot("14:00 - 16:00", baseAqi + 10));
        
        // Late afternoon (16-18 PM) - peak traffic
        timeSlots.add(createTimeSlot("16:00 - 18:00", baseAqi + 15));
        
        // Evening (18-20 PM) - traffic decreases
        timeSlots.add(createTimeSlot("18:00 - 20:00", baseAqi + 10));
        
        // Night (20-22 PM) - air quality improves
        timeSlots.add(createTimeSlot("20:00 - 22:00", baseAqi - 5));
        
        // Sort by AQI (best first)
        timeSlots.sort(Comparator.comparingInt(BestTimeSlotDTO::predictedAQI));
        
        // Return only top 5
        List<BestTimeSlotDTO> topSlots = timeSlots.stream()
                .limit(5)
                .toList();
        
        return new BestTimeResponseDTO(cityConfig.name(), cityId, topSlots);
    }
    
    private BestTimeSlotDTO createTimeSlot(String timeRange, int aqi) {
        // Ensure AQI is within valid range
        int validAqi = Math.max(0, Math.min(500, aqi));
        String color = com.aether.app.infrastructure.web.dto.AQIColorUtil.getAQIColor(validAqi);
        String recommendation = getRecommendationForAQI(validAqi);
        
        return new BestTimeSlotDTO(timeRange, validAqi, color, recommendation);
    }
    
    private String getRecommendationForAQI(int aqi) {
        if (aqi <= 50) {
            return "Excelente momento para salir. Aire limpio y saludable.";
        } else if (aqi <= 100) {
            return "Buen momento para actividades al aire libre. Calidad aceptable.";
        } else if (aqi <= 150) {
            return "Aceptable para la mayoría. Personas sensibles deben considerar limitar actividades prolongadas.";
        } else if (aqi <= 200) {
            return "Personas sensibles deben evitar actividades intensas. Público general debe limitar esfuerzos prolongados.";
        } else if (aqi <= 300) {
            return "No recomendado para actividades al aire libre. Considere ejercitarse en interiores.";
        } else {
            return "Evite actividades al aire libre. Aire peligroso para la salud.";
        }
    }
}
