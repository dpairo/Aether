package com.aether.app.infrastructure.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/api/hello")
    public String hello() {
        return "Hola, Dani!, angel es medio puto";
    }
}