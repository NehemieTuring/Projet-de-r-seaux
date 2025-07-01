package com.searchengine.model.dto.response;

import com.searchengine.model.enums.DocumentType;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for document response data.
 */
@Data
public class DocumentResponse {

    private String url;
    private String title;
    private String description;
    private DocumentType documentType;
    private Instant crawlTimestamp;
    private List<String> links;
    private List<String> mediaUrls;
    private Map<String, String> metadata;
    private double score;
}