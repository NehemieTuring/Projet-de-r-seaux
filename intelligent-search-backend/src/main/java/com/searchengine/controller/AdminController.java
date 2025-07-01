package com.searchengine.controller;

import com.searchengine.exception.SearchEngineException;
import com.searchengine.model.entity.Source;
import com.searchengine.repository.jpa.SourceJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for administrative operations.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "API for administrative tasks")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final SourceJpaRepository sourceJpaRepository;

    @Autowired
    public AdminController(SourceJpaRepository sourceJpaRepository) {
        this.sourceJpaRepository = sourceJpaRepository;
    }

    /**
     * Retrieves all sources.
     *
     * @return ResponseEntity with list of sources
     */
    @Operation(summary = "Get all sources", description = "Fetches all registered sources")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sources retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/sources")
    public ResponseEntity<List<Source>> getAllSources() {
        logger.info("Retrieving all sources");
        try {
            List<Source> sources = sourceJpaRepository.findAll();
            logger.debug("Retrieved {} sources", sources.size());
            return ResponseEntity.ok(sources);
        } catch (Exception e) {
            logger.error("Failed to retrieve sources", e);
            throw new SearchEngineException("Failed to retrieve sources", e);
        }
    }

    /**
     * Adds a new source.
     *
     * @param source The source to add
     * @return ResponseEntity with the added source
     */
    @Operation(summary = "Add a source", description = "Registers a new source for crawling")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Source added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid source data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/sources")
    public ResponseEntity<Source> addSource(@RequestBody Source source) {
        logger.info("Adding new source: {}", source.getUrl());
        try {
            Source savedSource = sourceJpaRepository.save(source);
            logger.debug("Source added: {}", savedSource.getUrl());
            return ResponseEntity.ok(savedSource);
        } catch (Exception e) {
            logger.error("Failed to add source: {}", source.getUrl(), e);
            throw new SearchEngineException("Failed to add source: " + source.getUrl(), e);
        }
    }
}