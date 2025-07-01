package com.smartcrawler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@Data
public class ClientCredentials {
    @JsonProperty("organization")
    private String organization;

    @JsonProperty("email")
    private String email;

    @JsonProperty("contactName")
    private String contactName;

    @JsonProperty("password")
    private String password;

    @JsonProperty("newPassword")
    private String newPassword;

    @JsonProperty("secretPhrase")
    private String secretPhrase;

    @JsonProperty("hint")
    private String hint;

    @JsonProperty("apiKey")
    private String apiKey;

    @JsonProperty("uuid")
    private UUID uuid;
}
