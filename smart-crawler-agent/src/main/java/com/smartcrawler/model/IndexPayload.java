package com.smartcrawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartcrawler.model.enums.CrawlStatus;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class IndexPayload {

    @JsonProperty("batchId")
    private String batchId;

    @JsonProperty("crawlSession")
    private String crawlSession;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("results")
    private List<CrawlResult> results;

    @JsonProperty("summary")
    private Summary summary;

    public IndexPayload() {
        this.batchId = UUID.randomUUID().toString();
        this.crawlSession = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.results = new ArrayList<>();
        this.summary = new Summary();
    }

    public void setResults(List<CrawlResult> results) {
        this.results = results;
        updateSummary();
    }

    public void addResult(CrawlResult result) {
        if (result != null) {
            results.add(result);
            updateSummary();
        }
    }

    private void updateSummary() {
        summary.setTotalUrls(results.size());
        summary.setSuccessCount(results.stream().filter(r -> r.getStatus() == CrawlStatus.SUCCESS).count());
        summary.setErrorCount(results.stream().filter(r -> r.getStatus() == CrawlStatus.ERROR).count());
        summary.setDynamicCount(results.stream().filter(CrawlResult::isDynamic).count());
    }

    @Data
    @NoArgsConstructor
    public static class Summary {
        @JsonProperty("totalUrls")
        private long totalUrls;

        @JsonProperty("successCount")
        private long successCount;

        @JsonProperty("errorCount")
        private long errorCount;

        @JsonProperty("dynamicCount")
        private long dynamicCount;
    }
}
