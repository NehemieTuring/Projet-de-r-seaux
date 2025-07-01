package com.searchengine.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.searchengine.model.enums.DocumentType;
import com.searchengine.model.enums.DocumentTypeDeserializer;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(indexName = "documents")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchDocument {

    @Id
    private String url;
    private String title;
    private String content;
    private String description;

    @JsonDeserialize(using = DocumentTypeDeserializer.class)
    @JsonProperty("documentType")
    private DocumentType documentType;

    private List<String> links = new ArrayList<>();
    private List<String> mediaUrls = new ArrayList<>();
    private List<Map<String, String>> apiEndpoints = new ArrayList<>();

    @JsonProperty("isDynamic")
    private boolean isDynamic;

    @JsonProperty("crawlTimestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant crawlTimestamp;

    private int httpStatus;
    private Map<String, String> metadata = new HashMap<>();
    private String contentHash;

    @JsonIgnore
    public String getLanguage() {
        return metadata != null ? metadata.get("language") : null;
    }

    @JsonIgnore
    public void setLanguage(String language) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if (language != null) {
            metadata.put("language", language);
        } else {
            metadata.remove("language");
        }
    }
}