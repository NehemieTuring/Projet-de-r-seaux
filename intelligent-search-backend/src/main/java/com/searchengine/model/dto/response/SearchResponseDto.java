package com.searchengine.model.dto.response;

import com.searchengine.model.entity.SearchContext;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseDto {
    private List<DocumentResponse> results;
    private long totalResults;
    private SearchContext context;
}