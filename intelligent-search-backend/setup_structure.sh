#!/bin/bash

# Dossier racine
BASE_DIR="src/main/java/com/searchengine"
RESOURCES_DIR="src/main/resources"

# Créer les dossiers Java
mkdir -p $BASE_DIR/config
mkdir -p $BASE_DIR/controller
mkdir -p $BASE_DIR/service
mkdir -p $BASE_DIR/model/entity
mkdir -p $BASE_DIR/model/dto/request
mkdir -p $BASE_DIR/model/dto/response
mkdir -p $BASE_DIR/model/enums
mkdir -p $BASE_DIR/repository/elasticsearch
mkdir -p $BASE_DIR/repository/jpa
mkdir -p $BASE_DIR/component/scoring
mkdir -p $BASE_DIR/component/context
mkdir -p $BASE_DIR/component/indexing
mkdir -p $BASE_DIR/component/monitoring
mkdir -p $BASE_DIR/exception
mkdir -p $BASE_DIR/security
mkdir -p $BASE_DIR/utils

# Créer les fichiers de configuration Java vides
touch $BASE_DIR/config/{ElasticsearchConfig.java,RedisConfig.java,CorsConfig.java,SecurityConfig.java,SwaggerConfig.java,JmxConfig.java}

# Créer les fichiers controllers vides
touch $BASE_DIR/controller/{SearchController.java,IndexController.java,AutocompleteController.java,AnalyticsController.java,HealthController.java,AdminController.java}

# Créer les fichiers services vides
touch $BASE_DIR/service/{SearchService.java,IndexingService.java,ScoringService.java,AutocompleteService.java,ContextService.java,AnalyticsService.java,CacheService.java,NotificationService.java}

# Créer les entités
touch $BASE_DIR/model/entity/{Document.java,Source.java,SearchQuery.java,SearchContext.java,SearchAnalytics.java,CrawlSession.java}

# DTO Request/Response
touch $BASE_DIR/model/dto/request/{SearchRequest.java,IndexRequest.java,AutocompleteRequest.java,AnalyticsRequest.java}
touch $BASE_DIR/model/dto/response/{SearchResponse.java,DocumentResponse.java,AutocompleteResponse.java,AnalyticsResponse.java,HealthResponse.java}

# Enums
touch $BASE_DIR/model/enums/{DocumentType.java,SourceType.java,SearchType.java,IndexStatus.java}

# Repositories
touch $BASE_DIR/repository/elasticsearch/{DocumentRepository.java,SearchRepository.java,AnalyticsRepository.java}
touch $BASE_DIR/repository/jpa/{SourceJpaRepository.java,ContextJpaRepository.java,AnalyticsJpaRepository.java}

# Components scoring
touch $BASE_DIR/component/scoring/{ScoreCalculator.java,PopularityScorer.java,FreshnessScorer.java,RelevanceScorer.java,LocationScorer.java}

# Components context
touch $BASE_DIR/component/context/{ContextAnalyzer.java,GeoLocationDetector.java,UserProfileBuilder.java,SessionTracker.java}

# Components indexing
touch $BASE_DIR/component/indexing/{DocumentProcessor.java,ContentAnalyzer.java,LanguageDetector.java,DuplicateDetector.java}

# Monitoring
touch $BASE_DIR/component/monitoring/{MetricsCollector.java,PerformanceMonitor.java,AlertManager.java}

# Exceptions
touch $BASE_DIR/exception/{SearchEngineException.java,IndexingException.java,SearchException.java,GlobalExceptionHandler.java}

# Sécurité
touch $BASE_DIR/security/{RateLimitFilter.java,ApiKeyAuthFilter.java,IpWhitelistFilter.java}

# Utils
touch $BASE_DIR/utils/{SearchUtils.java,ElasticsearchUtils.java,CacheUtils.java,GeoUtils.java,ValidationUtils.java}

# Ressources
mkdir -p $RESOURCES_DIR/elasticsearch/{mappings,settings}
mkdir -p $RESOURCES_DIR/sql
mkdir -p $RESOURCES_DIR/static/api-docs

touch $RESOURCES_DIR/application-dev.properties
touch $RESOURCES_DIR/application-prod.properties
touch $RESOURCES_DIR/elasticsearch/mappings/{document-mapping.json,analytics-mapping.json,autocomplete-mapping.json}
touch $RESOURCES_DIR/elasticsearch/settings/{document-settings.json,analyzer-settings.json}
touch $RESOURCES_DIR/sql/{schema.sql,data.sql}
