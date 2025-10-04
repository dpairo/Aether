package com.aether.app.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocationConsentRequest(
        @NotNull Boolean consent,
        @NotNull Double lat,
        @NotNull Double lon,
        Double accuracyMeters,
        @Size(max = 64) String source,
        @Size(max = 32) String consentVersion,
        @Pattern(regexp = "^[0-9TZ:.-]+$") String assertedAtIso
) {}