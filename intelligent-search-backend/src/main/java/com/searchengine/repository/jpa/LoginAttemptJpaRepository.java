package com.searchengine.repository.jpa;

import com.searchengine.model.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAttemptJpaRepository extends JpaRepository<LoginAttempt, Long> {
    List<LoginAttempt> findByEmailAndTimestampAfter(String email, LocalDateTime timestamp);
}