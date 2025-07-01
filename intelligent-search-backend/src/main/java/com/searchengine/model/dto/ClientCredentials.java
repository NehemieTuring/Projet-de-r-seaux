package com.searchengine.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ClientCredentials {
    private String organization;
    private String email;
    private String contactName;
    private String password;
    private String secretPhrase;
    private String hint;
    private UUID uuid;
    private String apiKey;
}