package com.searchengine.config;

import com.searchengine.security.IpWhitelistFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Configuration for Spring Security.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final IpWhitelistFilter ipWhitelistFilter;

    @Autowired
    public SecurityConfig(IpWhitelistFilter ipWhitelistFilter) {
        this.ipWhitelistFilter = ipWhitelistFilter;
    }

    /**
     * BCryptPasswordEncoder bean used in AuthService
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the security filter chain.
     *
     * @param http HttpSecurity configuration
     * @return SecurityFilterChain
     * @throws Exception If configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(ipWhitelistFilter, ChannelProcessingFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .httpBasic(httpBasic -> {});
        return http.build();
    }
}
