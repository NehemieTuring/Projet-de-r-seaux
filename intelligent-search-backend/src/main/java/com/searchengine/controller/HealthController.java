package com.searchengine.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.searchengine.model.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for health check endpoints.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "API for checking backend health")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    private final ElasticsearchClient elasticsearchClient;
    private final RedisConnectionFactory redisConnectionFactory;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HealthController(ElasticsearchClient elasticsearchClient,
                            RedisConnectionFactory redisConnectionFactory,
                            JdbcTemplate jdbcTemplate) {
        this.elasticsearchClient = elasticsearchClient;
        this.redisConnectionFactory = redisConnectionFactory;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Checks the health of the backend and its dependencies.
     *
     * @return ResponseEntity with health status
     */
    @Operation(summary = "Check backend health", description = "Verifies connectivity to Elasticsearch, Redis, and PostgreSQL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All services are healthy"),
            @ApiResponse(responseCode = "503", description = "One or more services are unhealthy")
    })
    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        logger.info("Performing health check");
        HealthResponse healthResponse = new HealthResponse();
        healthResponse.setStatus("UP");

        try {
            // Check Elasticsearch
            elasticsearchClient.cluster().health();
            healthResponse.addComponent("elasticsearch", "UP");
            logger.debug("Elasticsearch health check passed");
        } catch (Exception e) {
            healthResponse.setStatus("DOWN");
            healthResponse.addComponent("elasticsearch", "DOWN", e.getMessage());
            logger.error("Elasticsearch health check failed", e);
        }

        try {
            // Check Redis
            redisConnectionFactory.getConnection().ping();
            healthResponse.addComponent("redis", "UP");
            logger.debug("Redis health check passed");
        } catch (Exception e) {
            healthResponse.setStatus("DOWN");
            healthResponse.addComponent("redis", "DOWN", e.getMessage());
            logger.error("Redis health check failed", e);
        }

        try {
            // Check PostgreSQL
            jdbcTemplate.execute("SELECT 1");
            healthResponse.addComponent("postgresql", "UP");
            logger.debug("PostgreSQL health check passed");
        } catch (Exception e) {
            healthResponse.setStatus("DOWN");
            healthResponse.addComponent("postgresql", "DOWN", e.getMessage());
            logger.error("PostgreSQL health check failed", e);
        }

        HttpStatus status = "UP".equals(healthResponse.getStatus()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(healthResponse);
    }
}