package com.searchengine.controller;

import com.searchengine.model.dto.request.IndexRequest;
import com.searchengine.service.IndexingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling indexing requests from crawler agents.
 */
@RestController
@RequestMapping("/api/index")
@Tag(name = "Indexing", description = "API for indexing crawler data")
public class IndexController {

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
    private final IndexingService indexingService;

    public IndexController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    /**
     * Receives and processes indexing requests from crawler agents.
     *
     * @param indexRequest The indexing request payload
     * @return ResponseEntity with processing status
     */
    @Operation(summary = "Index crawler data", description = "Processes and indexes data from crawler agents")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Index request accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<String> indexData(@Valid @RequestBody IndexRequest indexRequest) {
        logger.info("Received indexing request with batchId: {}", indexRequest.getBatchId());
        logger.info("Received indexing request: {}", indexRequest);
        try {
            indexingService.processIndexRequest(indexRequest);
            logger.debug("Index request accepted for batchId: {}", indexRequest.getBatchId());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("Index request accepted for batchId: " + indexRequest.getBatchId());
        } catch (Exception e) {
            logger.error("Failed to process index request for batchId: {}", indexRequest.getBatchId(), e);
            throw e; // Handled by GlobalExceptionHandler
        }
    }
}