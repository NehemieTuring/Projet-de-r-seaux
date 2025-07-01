package com.searchengine.controller;

import com.searchengine.model.dto.ClientCredentials;
import com.searchengine.model.dto.LoginRequest;
import com.searchengine.service.AuthService;
import com.searchengine.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ClientCredentials> register(@RequestBody ClientCredentials credentials) {
        try {
            return ResponseEntity.ok(authService.register(credentials));
        } catch (AuthException e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ClientCredentials());
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<ClientCredentials> authenticate(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.authenticate(request));
        } catch (AuthException e) {
            logger.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(new ClientCredentials());
        }
    }

    @PostMapping("/reset-credentials")
    public ResponseEntity<ClientCredentials> resetCredentials(@RequestBody ClientCredentials credentials) {
        try {
            return ResponseEntity.ok(authService.resetCredentials(credentials, null));
        } catch (AuthException e) {
            logger.error("Credential reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ClientCredentials());
        }
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Void> validateToken(@RequestParam String uuid, @RequestHeader("X-API-Key") String apiKey) {
        if (authService.validateToken(uuid, apiKey)) {
            return ResponseEntity.ok().build();
        }
        logger.error("Token validation failed for UUID: {}", uuid);
        return ResponseEntity.status(401).build();
    }

    @PutMapping("/account")
    public ResponseEntity<ClientCredentials> updateAccount(@RequestBody ClientCredentials credentials, @RequestHeader("X-API-Key") String apiKey, @RequestParam String password) {
        try {
            return ResponseEntity.ok(authService.updateAccount(credentials, password));
        } catch (AuthException e) {
            logger.error("Account update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ClientCredentials());
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestParam String uuid, @RequestHeader("X-API-Key") String apiKey, @RequestParam String password) {
        try {
            authService.deleteAccount(uuid, apiKey, password);
            return ResponseEntity.noContent().build();
        } catch (AuthException e) {
            logger.error("Account deletion failed: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}