package com.searchengine.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Security filter to restrict access based on a whitelist of IP addresses.
 */
@Component
//@ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
public class IpWhitelistFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IpWhitelistFilter.class);

    @Value("${app.security.ip-whitelist:127.0.0.1,::1}")
    private Set<String> whitelist;

    /**
     * Filters requests based on the client's IP address.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param filterChain The filter chain
     * @throws ServletException If a servlet error occurs
     * @throws IOException If an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        logger.debug("Checking IP: {} against whitelist", clientIp);

        if (whitelist.contains(clientIp)) {
            logger.debug("IP {} is whitelisted", clientIp);
            filterChain.doFilter(request, response);
        } else {
            logger.warn("IP {} is not whitelisted, access denied", clientIp);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Access denied: IP not whitelisted");
        }
    }
}