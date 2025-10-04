package com.aether.app.infrastructure.web.dto;

public record WAQIDataDTO(
        Integer aqi,
        Integer idx,
        String dominentpol,
        WAQIIAQIDTO iaqi,
        WAQITimeDTO time,
        String city
) {}
