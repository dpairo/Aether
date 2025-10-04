package com.aether.app.infrastructure.web.dto;

import java.time.Instant;

public record LocationConsentResponse(
        Long id,
        boolean consent,
        Instant consentGivenAt,
        Instant consentRevokedAt,
        Double lat,
        Double lon,
        Double accuracyMeters,
        String source,
        String city,
        String state,
        String country
) {}