package com.aether.app.infrastructure.web.controller;

import com.aether.app.infrastructure.web.dto.AirSampleDTO;
import com.aether.app.infrastructure.web.dto.ForecastDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AirController {

    @GetMapping("/air/samples")
    public List<AirSampleDTO> samples(
            @RequestParam Long stationId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return List.of(
                new AirSampleDTO(stationId, "2025-10-04T10:00:00Z", 12.3, 40.1, 35.0, 52),
                new AirSampleDTO(stationId, "2025-10-04T11:00:00Z", 13.0, 39.2, 34.8, 51),
                new AirSampleDTO(stationId, "2025-10-04T12:00:00Z", 15.1, 38.0, 33.5, 55)
        );
    }

    @GetMapping("/air/forecast")
    public ForecastDTO forecast(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        return new ForecastDTO(lat, lon, "2025-10-04T18:00:00Z", "PM2_5", 17.2);
    }
}