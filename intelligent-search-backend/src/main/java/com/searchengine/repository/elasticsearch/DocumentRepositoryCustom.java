package com.searchengine.repository.elasticsearch;

import com.searchengine.model.entity.SearchDocument;
import com.searchengine.model.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

/**
 * Interface pour les méthodes de recherche personnalisées
 */
public interface DocumentRepositoryCustom {

    Page<SearchDocument> findByContentContainingOrTitleContaining(String content, String title, Pageable pageable);

    Page<SearchDocument> findByContentContainingOrTitleContainingAndDocumentType(
            String content, String title, DocumentType documentType, Pageable pageable);

    Page<SearchDocument> findByContentContainingOrTitleContainingAndLanguage(
            String content, String title, String language, Pageable pageable);

    Page<SearchDocument> findByContentContainingOrTitleContainingAndDocumentTypeAndLanguage(
            String content, String title, DocumentType documentType, String language, Pageable pageable);

    Page<SearchDocument> findByTitleStartingWithOrContentStartingWith(
            String title, String content, Pageable pageable);

    Page<SearchDocument> findByTitleStartingWithOrContentStartingWithAndLanguage(
            String title, String content, String language, Pageable pageable);

    Page<SearchDocument> findByTitleAutocomplete(
            String prefix, String language, Pageable pageable);

}