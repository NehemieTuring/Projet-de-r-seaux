package com.auth;

import com.smartcrawler.model.ClientCredentials;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import com.smartcrawler.utils.PasswordUtil;
import com.smartcrawler.utils.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrawler.service.BackendClient;
import java.util.UUID;
import java.util.regex.Pattern;

public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 300_000; // 5 minutes
    private int failedAttempts = 0;
    private long blockUntil = 0;
    private final AuthenticationConfig config;
    private final BackendClient backendClient;
    private final ObjectMapper objectMapper;

    public AuthenticationService(AuthenticationConfig config, BackendClient backendClient) {
        if (config == null || backendClient == null) {
            throw new IllegalArgumentException("Config or BackendClient cannot be null");
        }
        this.config = config;
        this.backendClient = backendClient;
        this.objectMapper = new ObjectMapper();
    }

    public ClientCredentials register(ClientCredentials credentials) throws Exception {
        checkBlockStatus();
        try {
            validateCredentials(credentials, true);
            if (!ValidationUtils.isValidUrl(config.getBackendRegisterUrl())) {
                throw new IllegalArgumentException("Invalid register URL: " + config.getBackendRegisterUrl());
            }

            credentials.setPassword(PasswordUtil.hashPassword(credentials.getPassword()));
            String response = backendClient.postRegister(config.getBackendRegisterUrl(), credentials, config.getApiKeyHeaderName());
            logger.info("Registration request sent to: " + config.getBackendRegisterUrl(), null);

            ClientCredentials result = objectMapper.readValue(response, ClientCredentials.class);
            validateResponse(result);
            logger.info("Registration successful, UUID: " + result.getUuid(), null);
            failedAttempts = 0;
            return result;
        } catch (Exception e) {
            incrementFailedAttempts();
            throw e;
        }
    }

    public ClientCredentials authenticate(ClientCredentials credentials) throws Exception {
        checkBlockStatus();
        try {
            validateCredentialsForLogin(credentials);
            if (!ValidationUtils.isValidUrl(config.getBackendAuthUrl())) {
                throw new IllegalArgumentException("Invalid auth URL: " + config.getBackendAuthUrl());
            }

            credentials.setPassword(PasswordUtil.hashPassword(credentials.getPassword()));
            String response = backendClient.postAuth(config.getBackendAuthUrl(), credentials, config.getApiKeyHeaderName());
            logger.info("Authentication request sent to: " + config.getBackendAuthUrl(), null);

            ClientCredentials result = objectMapper.readValue(response, ClientCredentials.class);
            validateResponse(result);
            logger.info("Authentication successful, UUID: " + result.getUuid(), null);
            failedAttempts = 0;
            return result;
        } catch (Exception e) {
            incrementFailedAttempts();
            throw e;
        }
    }

    public ClientCredentials resetCredentials(ClientCredentials credentials, String oldPassword) throws Exception {
        checkBlockStatus();
        try {
            validateCredentials(credentials, false);
            if (!ValidationUtils.isValidUrl(config.getBackendResetUrl())) {
                throw new IllegalArgumentException("Invalid reset URL: " + config.getBackendResetUrl());
            }

            if (oldPassword != null && !oldPassword.isEmpty()) {
                if (!PASSWORD_PATTERN.matcher(oldPassword).matches()) {
                    throw new IllegalArgumentException("Old password does not meet requirements");
                }
                credentials.setPassword(PasswordUtil.hashPassword(oldPassword));
                credentials.setNewPassword(PasswordUtil.hashPassword(credentials.getPassword()));
            } else if (credentials.getSecretPhrase() != null && !credentials.getSecretPhrase().isEmpty()) {
                credentials.setPassword(null);
                credentials.setNewPassword(PasswordUtil.hashPassword(credentials.getPassword()));
            } else {
                throw new IllegalArgumentException("Old password or secret phrase required for reset");
            }

            String response = backendClient.resetCredentials(config.getBackendResetUrl(), credentials, config.getApiKeyHeaderName());
            logger.info("Reset credentials request sent to: " + config.getBackendResetUrl(), null);

            ClientCredentials result = objectMapper.readValue(response, ClientCredentials.class);
            validateResponse(result);
            logger.info("Credentials reset successful, UUID: " + result.getUuid(), null);
            failedAttempts = 0;
            return result;
        } catch (Exception e) {
            incrementFailedAttempts();
            throw e;
        }
    }

    public ClientCredentials updateAccount(ClientCredentials credentials, String password) throws Exception {
        checkBlockStatus();
        try {
            validateCredentials(credentials, false);
            if (!ValidationUtils.isValidUrl(config.getBackendUpdateAccountUrl())) {
                throw new IllegalArgumentException("Invalid update account URL: " + config.getBackendUpdateAccountUrl());
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password required for updating account");
            }
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                throw new IllegalArgumentException("Password does not meet requirements");
            }

            credentials.setPassword(PasswordUtil.hashPassword(password));
            String response = backendClient.updateAccount(config.getBackendUpdateAccountUrl(), credentials, config.getApiKeyHeaderName());
            logger.info("Update account request sent to: " + config.getBackendUpdateAccountUrl(), null);

            ClientCredentials result = objectMapper.readValue(response, ClientCredentials.class);
            validateResponse(result);
            logger.info("Account update successful, UUID: " + result.getUuid(), null);
            failedAttempts = 0;
            return result;
        } catch (Exception e) {
            incrementFailedAttempts();
            throw e;
        }
    }

    public void deleteAccount(String uuid, String apiKey, String password) throws Exception {
        checkBlockStatus();
        try {
            if (uuid == null || apiKey == null || password == null || uuid.isEmpty() || apiKey.isEmpty() || password.isEmpty()) {
                throw new IllegalArgumentException("UUID, API key, and password are required");
            }
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                throw new IllegalArgumentException("Password does not meet requirements");
            }
            if (!ValidationUtils.isValidUrl(config.getBackendDeleteAccountUrl())) {
                throw new IllegalArgumentException("Invalid delete account URL: " + config.getBackendDeleteAccountUrl());
            }

            ClientCredentials credentials = new ClientCredentials();
            credentials.setUuid(UUID.fromString(uuid));
            credentials.setApiKey(apiKey);
            credentials.setPassword(PasswordUtil.hashPassword(password));

            backendClient.deleteAccount(config.getBackendDeleteAccountUrl(), credentials, config.getApiKeyHeaderName());
            logger.info("Account deletion request sent to: " + config.getBackendDeleteAccountUrl(), null);
            logger.info("Account deleted successfully, UUID: " + uuid, null);
            failedAttempts = 0;
        } catch (Exception e) {
            incrementFailedAttempts();
            throw e;
        }
    }

    public boolean validateToken(String uuid, String apiKey) throws Exception {
        if (uuid == null || apiKey == null || uuid.isEmpty() || apiKey.isEmpty()) {
            return false;
        }
        if (!ValidationUtils.isValidUrl(config.getBackendValidateTokenUrl())) {
            throw new IllegalArgumentException("Invalid validate token URL: " + config.getBackendValidateTokenUrl());
        }

        ClientCredentials credentials = new ClientCredentials();
        credentials.setUuid(UUID.fromString(uuid));
        credentials.setApiKey(apiKey);

        String response = backendClient.validateToken(config.getBackendValidateTokenUrl(), credentials, config.getApiKeyHeaderName());
        logger.info("Token validation request sent to: " + config.getBackendValidateTokenUrl(), null);
        return Boolean.parseBoolean(response);
    }

    private void validateCredentials(ClientCredentials credentials, boolean isRegistration) throws IllegalArgumentException {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials cannot be null");
        }
        if (credentials.getOrganization() == null || credentials.getOrganization().isEmpty()) {
            throw new IllegalArgumentException("Organization name is required");
        }
        if (credentials.getEmail() == null || credentials.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(credentials.getEmail()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (credentials.getContactName() == null || credentials.getContactName().isEmpty()) {
            throw new IllegalArgumentException("Contact name is required");
        }
        if (!isRegistration && credentials.getPassword() != null && !credentials.getPassword().isEmpty()) {
            if (!PASSWORD_PATTERN.matcher(credentials.getPassword()).matches()) {
                throw new IllegalArgumentException("Password must contain at least 8 characters, including letters, numbers, and special characters");
            }
        }
        if (isRegistration) {
            if (credentials.getPassword() == null || credentials.getPassword().isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }
            if (!PASSWORD_PATTERN.matcher(credentials.getPassword()).matches()) {
                throw new IllegalArgumentException("Password must contain at least 8 characters, including letters, numbers, and special characters");
            }
            if (credentials.getSecretPhrase() == null || credentials.getSecretPhrase().isEmpty()) {
                throw new IllegalArgumentException("Secret phrase is required for registration");
            }
            if (credentials.getHint() == null || credentials.getHint().isEmpty()) {
                throw new IllegalArgumentException("Hint is required for registration");
            }
        }
    }

    private void validateCredentialsForLogin(ClientCredentials credentials) throws IllegalArgumentException {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials cannot be null");
        }
        if (credentials.getOrganization() == null || credentials.getOrganization().isEmpty()) {
            throw new IllegalArgumentException("Organization name is required");
        }
        if (credentials.getEmail() == null || credentials.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(credentials.getEmail()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (credentials.getPassword() == null || credentials.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (!PASSWORD_PATTERN.matcher(credentials.getPassword()).matches()) {
            throw new IllegalArgumentException("Password must contain at least 8 characters, including letters, numbers, and special characters");
        }
    }

    private void validateResponse(ClientCredentials result) throws Exception {
        if (result == null || result.getUuid() == null || result.getApiKey() == null) {
            throw new Exception("Invalid response from backend: missing UUID or API key");
        }
    }

    private void checkBlockStatus() throws IllegalStateException {
        if (System.currentTimeMillis() < blockUntil) {
            long remaining = (blockUntil - System.currentTimeMillis()) / 1000;
            throw new IllegalStateException("Too many failed attempts. Try again in " + remaining + " seconds.");
        }
    }

    private void incrementFailedAttempts() {
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) {
            blockUntil = System.currentTimeMillis() + BLOCK_DURATION_MS;
            logger.warn("Too many failed attempts, blocked until: " + new java.util.Date(blockUntil), null);
        }
    }
}