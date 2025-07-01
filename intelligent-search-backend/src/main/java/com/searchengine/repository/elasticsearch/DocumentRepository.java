package com.searchengine.repository.elasticsearch;

import com.searchengine.model.entity.SearchDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentRepository extends ElasticsearchRepository<SearchDocument, String>, DocumentRepositoryCustom {

    // Méthodes natives Elasticsearch (gardées telles quelles)
    boolean existsByContentHash(String contentHash);
    boolean existsByUrl(String url);
    Slice<SearchDocument> findByUrl(String url, PageRequest pageRequest);
}