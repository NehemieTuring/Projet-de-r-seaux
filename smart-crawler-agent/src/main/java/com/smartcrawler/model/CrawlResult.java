package com.smartcrawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartcrawler.model.enums.DocumentType;
import com.smartcrawler.model.enums.CrawlStatus;
import lombok.*;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlResult {

    @JsonProperty("url")
    private String url;

    @JsonProperty("status")
    private CrawlStatus status;

    @JsonProperty("documentType")
    private DocumentType documentType;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("content")
    private String content;

    @JsonProperty("links")
    @Builder.Default
    private Set<String> links = new HashSet<>();

    @JsonProperty("mediaUrls")
    @Builder.Default
    private Set<String> mediaUrls = new HashSet<>();

    @JsonProperty("apiEndpoints")
    @Builder.Default
    private List<ApiEndpoint> apiEndpoints = new ArrayList<>();

    @JsonProperty("isDynamic")
    private boolean isDynamic;

    @JsonProperty("crawlTimestamp")
    private long crawlTimestamp;

    @JsonProperty("responseTime")
    private long responseTime;

    @JsonProperty("httpStatus")
    private int httpStatus;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiEndpoint {
        @JsonProperty("url")
        private String url;

        @JsonProperty("method")
        private String method;
    }
}
