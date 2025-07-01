package com.auth;

import com.smartcrawler.core.ConfigManager;

public class AuthenticationConfig {
    private final String backendAuthUrl;
    private final String backendRegisterUrl;
    private final String backendResetUrl;
    private final String backendValidateTokenUrl;
    private final String backendUpdateAccountUrl;
    private final String backendDeleteAccountUrl;
    private final String apiKeyHeaderName = "X-API-Key";

    public AuthenticationConfig(ConfigManager configManager) {
        if (configManager == null) {
            throw new IllegalArgumentException("ConfigManager cannot be null");
        }
        this.backendAuthUrl = configManager.getProperty("backend.com.auth.url", "http://localhost:8080/api/auth");
        this.backendRegisterUrl = configManager.getProperty("backend.register.url", "http://localhost:8080/api/register");
        this.backendResetUrl = configManager.getProperty("backend.reset.url", "http://localhost:8080/api/reset-credentials");
        this.backendValidateTokenUrl = configManager.getProperty("backend.validate.token.url", "http://localhost:8080/api/validate-token");
        this.backendUpdateAccountUrl = configManager.getProperty("backend.update.account.url", "http://localhost:8080/api/account");
        this.backendDeleteAccountUrl = configManager.getProperty("backend.delete.account.url", "http://localhost:8080/api/account");
    }

    public String getBackendAuthUrl() {
        return backendAuthUrl;
    }

    public String getBackendRegisterUrl() {
        return backendRegisterUrl;
    }

    public String getBackendResetUrl() {
        return backendResetUrl;
    }

    public String getBackendValidateTokenUrl() {
        return backendValidateTokenUrl;
    }

    public String getBackendUpdateAccountUrl() {
        return backendUpdateAccountUrl;
    }

    public String getBackendDeleteAccountUrl() {
        return backendDeleteAccountUrl;
    }

    public String getApiKeyHeaderName() {
        return apiKeyHeaderName;
    }
}
