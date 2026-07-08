package com.matheus.procurement.controller;

import com.matheus.procurement.dto.RegisterRequest;
import com.matheus.procurement.entity.User;
import com.matheus.procurement.repository.UserRepository;
import com.matheus.procurement.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtTokenProvider jwtTokenProvider, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid Credentials");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid Credentials");
        }
        String token = jwtTokenProvider.generateToken(user.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        String encoded = passwordEncoder.encode(request.getPassword());
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(encoded);
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered");
    }

    public record LoginRequest(String email, String password) {}

}
