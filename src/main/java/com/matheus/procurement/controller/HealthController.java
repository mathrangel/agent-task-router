package com.matheus.procurement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.lang.management.ManagementFactory;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/health/details")
    public Map<String, Object> detailed() {
        return Map.of(
        "status", "ok",
        "version", "dev",
        "uptime", ManagementFactory.getRuntimeMXBean().getUptime()
        );
    }

}
