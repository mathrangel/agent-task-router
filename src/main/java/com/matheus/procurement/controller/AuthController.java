package com.matheus.procurement.controller;

import com.matheus.procurement.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if("admin@test.com".equals(request.email()) && "123456".equals(request.password())) {
            String token = jwtTokenProvider.generateToken(request.email());
            return ResponseEntity.ok(Map.of("token", token));
        }
            return ResponseEntity.status(401).body("Invalid Credentials");
    }

    public record LoginRequest(String email, String password) {}
}
