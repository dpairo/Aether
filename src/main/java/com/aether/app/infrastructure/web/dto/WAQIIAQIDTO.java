package com.aether.app.infrastructure.web.dto;

public record WAQIIAQIDTO(
        WAQIPollutantDTO pm25,
        WAQIPollutantDTO pm10,
        WAQIPollutantDTO no2,
        WAQIPollutantDTO o3,
        WAQIPollutantDTO co,
        WAQIPollutantDTO so2
) {}
