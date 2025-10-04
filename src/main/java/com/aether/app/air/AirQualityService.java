package com.aether.app.air;

import com.aether.app.infrastructure.web.dto.CityAirQualityDTO;
import com.aether.app.infrastructure.web.dto.ProvinceAirQualityDTO;
import com.aether.app.infrastructure.web.dto.WAQIResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class AirQualityService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    @Value("${air-quality.waqi.base-url}")
    private String baseUrl;
    
    @Value("${air-quality.waqi.token}")
    private String token;
    
    @Value("${air-quality.waqi.timeout:5000}")
    private int timeout;

    public AirQualityService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(10);
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

    public List<CityAirQualityDTO> getAllSpanishCitiesAirQuality() {
        List<CityConfig> cities = getSpanishCities();
        
        List<CompletableFuture<CityAirQualityDTO>> futures = cities.stream()
                .map(city -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return getAirQualityByCity(city.cityId());
                    } catch (Exception e) {
                        // Return a default response for failed cities
                        return createDefaultAirQuality(city);
                    }
                }, executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private CityAirQualityDTO createDefaultAirQuality(CityConfig city) {
        return new CityAirQualityDTO(
                city.name(),
                city.cityId(),
                city.latitude(),
                city.longitude(),
                null, // AQI unknown
                "Unknown",
                com.aether.app.infrastructure.web.dto.AQIColorUtil.getAQIColor(null), // Gray color for unknown
                "Unknown",
                new com.aether.app.infrastructure.web.dto.AirQualityDataDTO(
                        null, null, null, null, null, null
                ),
                new Date().toString()
        );
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
                new CityConfig("Zaragoza", "zaragoza", 41.6488, -0.8891)
        );
    }

    public List<ProvinceAirQualityDTO> getAllSpanishProvincesAirQuality() {
        List<ProvinceConfig> provinces = getSpanishProvinces();
        
        List<CompletableFuture<ProvinceAirQualityDTO>> futures = provinces.stream()
                .map(province -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // Get the main city of the province for AQI data
                        String mainCityId = province.mainCityId();
                        CityAirQualityDTO cityData = getAirQualityByCity(mainCityId);
                        
                        return ProvinceAirQualityDTO.fromCityData(
                                province.name(),
                                province.code(),
                                province.latitude(),
                                province.longitude(),
                                cityData
                        );
                    } catch (Exception e) {
                        // Return a default response for failed provinces
                        return createDefaultProvinceAirQuality(province);
                    }
                }, executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public List<CityAirQualityDTO> getCitiesByProvince(String provinceCode) {
        List<CityConfig> citiesInProvince = getCitiesByProvinceCode(provinceCode);
        
        List<CompletableFuture<CityAirQualityDTO>> futures = citiesInProvince.stream()
                .map(city -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return getAirQualityByCity(city.cityId());
                    } catch (Exception e) {
                        // Return a default response for failed cities
                        return createDefaultAirQuality(city);
                    }
                }, executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private ProvinceAirQualityDTO createDefaultProvinceAirQuality(ProvinceConfig province) {
        return new ProvinceAirQualityDTO(
                province.name(),
                province.code(),
                province.latitude(),
                province.longitude(),
                null, // AQI unknown
                "Unknown",
                com.aether.app.infrastructure.web.dto.AQIColorUtil.getAQIColor(null), // Gray color for unknown
                "Unknown",
                new com.aether.app.infrastructure.web.dto.AirQualityDataDTO(
                        null, null, null, null, null, null
                ),
                new Date().toString()
        );
    }

    private List<ProvinceConfig> getSpanishProvinces() {
        return List.of(
                new ProvinceConfig("Madrid", "M", 40.4168, -3.7038, "madrid"),
                new ProvinceConfig("Barcelona", "B", 41.3851, 2.1734, "barcelona"),
                new ProvinceConfig("Valencia", "V", 39.4699, -0.3763, "valencia"),
                new ProvinceConfig("Sevilla", "SE", 37.3891, -5.9845, "sevilla"),
                new ProvinceConfig("Bilbao", "BI", 43.2627, -2.9253, "bilbao"),
                new ProvinceConfig("Zaragoza", "Z", 41.6488, -0.8891, "zaragoza"),
                new ProvinceConfig("Málaga", "MA", 36.7213, -4.4214, "malaga"),
                new ProvinceConfig("Murcia", "MU", 37.9922, -1.1307, "murcia"),
                new ProvinceConfig("Palma", "PM", 39.5696, 2.6502, "palma"),
                new ProvinceConfig("Las Palmas", "GC", 28.1248, -15.4300, "las-palmas"),
                new ProvinceConfig("Alicante", "A", 38.3452, -0.4810, "alicante"),
                new ProvinceConfig("Córdoba", "CO", 37.8847, -4.7791, "cordoba"),
                new ProvinceConfig("Valladolid", "VA", 41.6523, -4.7245, "valladolid"),
                new ProvinceConfig("Vigo", "PO", 42.2406, -8.7207, "vigo"),
                new ProvinceConfig("Gijón", "O", 43.5453, -5.6619, "gijon")
        );
    }

    private List<CityConfig> getCitiesByProvinceCode(String provinceCode) {
        Map<String, List<CityConfig>> citiesByProvince = Map.of(
                "M", List.of(
                        new CityConfig("Madrid", "madrid", 40.4168, -3.7038),
                        new CityConfig("Alcalá de Henares", "alcala-henares", 40.4817, -3.3641),
                        new CityConfig("Getafe", "getafe", 40.3047, -3.7307)
                ),
                "B", List.of(
                        new CityConfig("Barcelona", "barcelona", 41.3851, 2.1734),
                        new CityConfig("L'Hospitalet", "hospitalet", 41.3596, 2.1004),
                        new CityConfig("Badalona", "badalona", 41.4500, 2.2474)
                ),
                "V", List.of(
                        new CityConfig("Valencia", "valencia", 39.4699, -0.3763),
                        new CityConfig("Gandía", "gandia", 38.9690, -0.1852),
                        new CityConfig("Sagunto", "sagunto", 39.6800, -0.2800)
                ),
                "SE", List.of(
                        new CityConfig("Sevilla", "sevilla", 37.3891, -5.9845),
                        new CityConfig("Dos Hermanas", "dos-hermanas", 37.2833, -5.9167),
                        new CityConfig("Alcalá de Guadaíra", "alcala-guadaira", 37.3333, -5.8500)
                ),
                "BI", List.of(
                        new CityConfig("Bilbao", "bilbao", 43.2627, -2.9253),
                        new CityConfig("Barakaldo", "barakaldo", 43.2964, -2.9889),
                        new CityConfig("Getxo", "getxo", 43.3500, -3.0167)
                ),
                "Z", List.of(
                        new CityConfig("Zaragoza", "zaragoza", 41.6488, -0.8891),
                        new CityConfig("Calatayud", "calatayud", 41.3500, -1.6333),
                        new CityConfig("Ejea de los Caballeros", "ejea", 42.1333, -1.1333)
                )
        );
        
        return citiesByProvince.getOrDefault(provinceCode, List.of());
    }

    private record CityConfig(String name, String cityId, Double latitude, Double longitude) {}
    private record ProvinceConfig(String name, String code, Double latitude, Double longitude, String mainCityId) {}
}
