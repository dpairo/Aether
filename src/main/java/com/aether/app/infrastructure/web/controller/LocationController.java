package com.aether.app.infrastructure.web.controller;

import com.aether.app.infrastructure.web.dto.LocationConsentRequest;
import com.aether.app.infrastructure.web.dto.LocationConsentResponse;
import com.aether.app.location.LocationConsent;
import com.aether.app.location.LocationService;
import com.aether.app.location.ReverseGeocodingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/location")
public class LocationController {

    private final LocationService service;
    private final ReverseGeocodingService reverse;

    public LocationController(LocationService service, ReverseGeocodingService reverse) {
        this.service = service;
        this.reverse = reverse;
    }

    @PostMapping("/consent")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationConsentResponse createConsent(
            @Valid @RequestBody LocationConsentRequest req,
            HttpServletRequest http
    ) {
        if (!Boolean.TRUE.equals(req.consent())) {
            throw new IllegalArgumentException("Se requiere consentimiento explícito (consent=true).");
        }

        // 1) Guardar base
        LocationConsent lc = new LocationConsent();
        lc.setConsent(true);
        lc.setLatitude(req.lat());
        lc.setLongitude(req.lon());
        lc.setAccuracyMeters(req.accuracyMeters());
        lc.setSource(req.source());
        lc.setConsentVersion(req.consentVersion());
        lc.setUserAgent(http.getHeader("User-Agent"));
        lc.setIpAddress(extractClientIp(http));

        if (req.assertedAtIso() != null && !req.assertedAtIso().isBlank()) {
            try { lc.setAssertedAt(Instant.parse(req.assertedAtIso())); }
            catch (DateTimeParseException ignored) {}
        }

        var saved = service.saveConsent(lc);

        // 2) Enriquecer con reverse geocoding (bloqueante y simple)
        try {
            var rr = reverse.reverse(req.lat(), req.lon(),
                    http.getHeader("Accept-Language"));
            saved.setCity(rr.city());
            saved.setState(rr.state());
            saved.setCountry(rr.country());
            service.saveConsent(saved); // persistir enriquecido
        } catch (Exception ex) {
            // Si falla Nominatim, seguimos adelante sin romper el flujo
        }

        return new LocationConsentResponse(
                saved.getId(),
                saved.isConsent(),
                saved.getConsentGivenAt(),
                saved.getConsentRevokedAt(),
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getAccuracyMeters(),
                saved.getSource(),
                saved.getCity(),
                saved.getState(),
                saved.getCountry()
        );
    }

    @GetMapping("/latest")
    public LocationConsentResponse latest() {
        var lc = service.latestActive()
                .orElseThrow(() -> new IllegalStateException("No hay localización con consentimiento activo."));
        return new LocationConsentResponse(
                lc.getId(),
                lc.isConsent(),
                lc.getConsentGivenAt(),
                lc.getConsentRevokedAt(),
                lc.getLatitude(),
                lc.getLongitude(),
                lc.getAccuracyMeters(),
                lc.getSource(),
                lc.getCity(),
                lc.getState(),
                lc.getCountry()
        );
    }

    @PostMapping("/revoke/{id}")
    public LocationConsentResponse revoke(@PathVariable Long id) {
        var lc = service.revoke(id).orElseThrow(() -> new IllegalArgumentException("ID no encontrado."));
        return new LocationConsentResponse(
                lc.getId(),
                lc.isConsent(),
                lc.getConsentGivenAt(),
                lc.getConsentRevokedAt(),
                lc.getLatitude(),
                lc.getLongitude(),
                lc.getAccuracyMeters(),
                lc.getSource(),
                lc.getCity(),
                lc.getState(),
                lc.getCountry()
        );
    }

    private String extractClientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}