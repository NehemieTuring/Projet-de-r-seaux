package com.searchengine.model.dto.request;

import com.searchengine.model.entity.SearchDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

/**
 * DTO for receiving indexing requests from the crawler agent.
 */
@Data
@ToString
public class IndexRequest {

    @NotBlank(message = "Batch ID cannot be blank")
    @Size(max = 36, message = "Batch ID must be a valid UUID")
    private String batchId;

    @NotBlank(message = "Crawl session cannot be blank")
    @Size(max = 36, message = "Crawl session must be a valid UUID")
    private String crawlSession;

    @NotNull(message = "Timestamp cannot be null")
    private Instant timestamp;

    @NotNull(message = "Results cannot be null")
    @Size(min = 1, max = 100, message = "Results size must be between 1 and 100")
    private List<SearchDocument> results;

    @NotNull(message = "Summary cannot be null")
    private Summary summary;

    /**
     * Summary of the crawl batch.
     */
    @Data
    public static class Summary {
        private int totalUrls;
        private int successCount;
        private int errorCount;
        private int dynamicCount;
    }
}