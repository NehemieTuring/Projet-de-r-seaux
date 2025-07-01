package com.searchengine.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String organization;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String password;

    @Column
    private String secretPhrase;

    @Column
    private String hint;

    @Column(nullable = false)
    private String apiKey;
}