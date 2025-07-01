package com.searchengine.service;

import com.searchengine.model.dto.ClientCredentials;
import com.searchengine.model.dto.LoginRequest;
import com.searchengine.model.entity.LoginAttempt;
import com.searchengine.model.entity.User;
import com.searchengine.repository.jpa.LoginAttemptJpaRepository;
import com.searchengine.repository.jpa.UserJpaRepository;
import com.searchengine.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserJpaRepository userRepository;
    private final LoginAttemptJpaRepository loginAttemptRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public ClientCredentials register(ClientCredentials credentials) {
        logger.info("Attempting to register user: {}", credentials.getEmail());
        if (userRepository.findByEmail(credentials.getEmail()).isPresent()) {
            throw new AuthException("Email already exists");
        }

        User user = new User();
        user.setOrganization(credentials.getOrganization());
        user.setEmail(credentials.getEmail());
        user.setContactName(credentials.getContactName());
        user.setPassword(passwordEncoder.encode(credentials.getPassword()));
        user.setSecretPhrase(passwordEncoder.encode(credentials.getSecretPhrase()));
        user.setHint(credentials.getHint());
        user.setApiKey(UUID.randomUUID().toString());
        user.setUuid(UUID.randomUUID());

        userRepository.save(user);
        logger.info("User registered successfully: {}", user.getEmail());

        credentials.setUuid(user.getUuid());
        credentials.setApiKey(user.getApiKey());
        credentials.setPassword(null);
        credentials.setSecretPhrase(null);
        return credentials;
    }

    @Transactional
    public ClientCredentials authenticate(LoginRequest request) {
        logger.info("Attempting to authenticate user: {}", request.getEmail());
        checkLoginAttempts(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginAttempt(request.getEmail(), false);
            throw new AuthException("Invalid credentials");
        }

        recordLoginAttempt(request.getEmail(), true);
        logger.info("User authenticated successfully: {}", request.getEmail());

        ClientCredentials credentials = new ClientCredentials();
        credentials.setOrganization(user.getOrganization());
        credentials.setEmail(user.getEmail());
        credentials.setContactName(user.getContactName());
        credentials.setUuid(user.getUuid());
        credentials.setApiKey(user.getApiKey());
        return credentials;
    }

    @Transactional
    public ClientCredentials resetCredentials(ClientCredentials credentials, String oldPassword) {
        logger.info("Attempting to reset credentials for: {}", credentials.getEmail());
        User user = userRepository.findByEmail(credentials.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));

        if ("dummy".equals(credentials.getPassword())) {
            if (!passwordEncoder.matches(credentials.getSecretPhrase(), user.getSecretPhrase())) {
                throw new AuthException("Invalid secret phrase");
            }
            credentials.setHint(user.getHint());
            return credentials;
        }

        if (!passwordEncoder.matches(credentials.getSecretPhrase(), user.getSecretPhrase())) {
            throw new AuthException("Invalid secret phrase");
        }

        user.setPassword(passwordEncoder.encode(credentials.getPassword()));
        user.setApiKey(UUID.randomUUID().toString());
        userRepository.save(user);
        logger.info("Credentials reset successfully for: {}", credentials.getEmail());

        credentials.setUuid(user.getUuid());
        credentials.setApiKey(user.getApiKey());
        credentials.setPassword(null);
        credentials.setSecretPhrase(null);
        return credentials;
    }

    public boolean validateToken(String uuid, String apiKey) {
        logger.debug("Validating token for UUID: {}", uuid);
        return userRepository.findByApiKey(apiKey)
                .map(user -> user.getUuid().toString().equals(uuid))
                .orElse(false);
    }

    @Transactional
    public ClientCredentials updateAccount(ClientCredentials credentials, String password) {
        logger.info("Attempting to update account for: {}", credentials.getEmail());
        User user = userRepository.findByEmail(credentials.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthException("Invalid password");
        }

        user.setOrganization(credentials.getOrganization());
        user.setEmail(credentials.getEmail());
        user.setContactName(credentials.getContactName());
        userRepository.save(user);
        logger.info("Account updated successfully for: {}", credentials.getEmail());

        credentials.setUuid(user.getUuid());
        credentials.setApiKey(user.getApiKey());
        return credentials;
    }

    @Transactional
    public void deleteAccount(String uuid, String apiKey, String password) {
        logger.info("Attempting to delete account for UUID: {}", uuid);
        User user = userRepository.findByApiKey(apiKey)
                .filter(u -> u.getUuid().toString().equals(uuid))
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthException("Invalid password");
        }

        userRepository.delete(user);
        logger.info("Account deleted successfully for UUID: {}", uuid);
    }

    private void checkLoginAttempts(String email) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES);
        List<LoginAttempt> attempts = loginAttemptRepository.findByEmailAndTimestampAfter(email, cutoff);
        long failedAttempts = attempts.stream().filter(a -> !a.isSuccess()).count();

        if (failedAttempts >= MAX_LOGIN_ATTEMPTS) {
            throw new AuthException("Too many failed login attempts. Try again later.");
        }
    }

    private void recordLoginAttempt(String email, boolean success) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setEmail(email);
        attempt.setTimestamp(LocalDateTime.now());
        attempt.setSuccess(success);
        loginAttemptRepository.save(attempt);
    }
}