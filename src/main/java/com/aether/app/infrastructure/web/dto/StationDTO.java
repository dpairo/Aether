package com.aether.app.infrastructure.web.dto;

public record StationDTO(
        Long id,
        String name,
        double lat,
        double lon
) {}