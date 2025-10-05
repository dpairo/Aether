package com.aether.app.infrastructure.web.dto;

import java.util.List;

/**
 * DTO representing the response with best time slots to go out
 */
public record BestTimeResponseDTO(
        String city,
        String cityId,
        List<BestTimeSlotDTO> bestTimeSlots
) {}

