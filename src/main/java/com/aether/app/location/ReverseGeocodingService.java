package com.aether.app.location;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class ReverseGeocodingService {

    private final RestClient client;

    public ReverseGeocodingService() {
        this.client = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "AetherHackathon/1.0 (contact: you@example.com)")
                .build();
    }

    public ReverseResult reverse(double lat, double lon, String lang) {
        Map<?, ?> json = client.get()
                .uri(uri -> uri.path("/reverse")
                        .queryParam("format", "jsonv2")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("addressdetails", 1)
                        .queryParam("zoom", 10) // nivel ciudad
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Accept-Language", lang != null ? lang : "es")
                .retrieve()
                .body(Map.class);

        Map<?, ?> addr = json != null ? (Map<?, ?>) json.get("address") : null;
        String city = firstNonNull(addr, "city", "town", "village", "hamlet", "municipality", "suburb", "locality");
        String state = addr != null ? (String) addr.get("state") : null;
        String country = addr != null ? (String) addr.get("country") : null;

        return new ReverseResult(city, state, country);
    }

    private String firstNonNull(Map<?, ?> addr, String... keys) {
        if (addr == null) return null;
        for (String k : keys) {
            Object v = addr.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }

    public record ReverseResult(String city, String state, String country) {}
}