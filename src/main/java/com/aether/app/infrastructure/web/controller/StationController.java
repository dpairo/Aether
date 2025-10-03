package com.aether.app.infrastructure.web.controller;

import com.aether.app.infrastructure.web.dto.StationDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StationController {

    @GetMapping("/stations")
    public List<StationDTO> listStations() {
        return List.of(
                new StationDTO(1L, "Madrid Centro", 40.4168, -3.7038),
                new StationDTO(2L, "Sevilla Norte", 37.3891, -5.9845)
        );
    }
}