package com.aether.app.infrastructure.web.dto;

public record WAQIResponseDTO(
        String status,
        WAQIDataDTO data
) {}
