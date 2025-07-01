package com.searchengine;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PreDestroy;

/**
 * Point d'entrée principal pour l'application backend du moteur de recherche intelligent.
 * Initialise l'application Spring Boot et vérifie la connectivité aux services externes.
 */
@SpringBootApplication
public class SearchEngineApplication {

	private static final Logger logger = LoggerFactory.getLogger(SearchEngineApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SearchEngineApplication.class, args);
		logger.info("Intelligent Search Engine Backend démarré avec succès");
	}

	/**
	 * Vérifie la connectivité à Elasticsearch, Redis et PostgreSQL au démarrage.
	 *
	 * @param elasticsearchClient Client Elasticsearch
	 * @param redisConnectionFactory Factory de connexion Redis
	 * @param jdbcTemplate Template JDBC pour PostgreSQL
	 * @return CommandLineRunner pour les vérifications au démarrage
	 */
	@Bean
	public CommandLineRunner startupCheck(ElasticsearchClient elasticsearchClient,
										  RedisConnectionFactory redisConnectionFactory,
										  JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				// Vérifier la connectivité à Elasticsearch
				elasticsearchClient.ping();
				logger.info("Connexion à Elasticsearch réussie");
			} catch (Exception e) {
				logger.error("Échec de la connexion à Elasticsearch", e);
				throw new IllegalStateException("Connexion à Elasticsearch échouée", e);
			}

			try {
				// Vérifier la connectivité à Redis
				redisConnectionFactory.getConnection().ping();
				logger.info("Connexion à Redis réussie");
			} catch (Exception e) {
				logger.error("Échec de la connexion à Redis", e);
				throw new IllegalStateException("Connexion à Redis échouée", e);
			}

			try {
				// Vérifier la connectivité à PostgreSQL
				jdbcTemplate.execute("SELECT 1");
				logger.info("Connexion à PostgreSQL réussie");
			} catch (Exception e) {
				logger.error("Échec de la connexion à PostgreSQL", e);
				throw new IllegalStateException("Connexion à PostgreSQL échouée", e);
			}
		};
	}

	/**
	 * Hook d'arrêt gracieux pour nettoyer les ressources.
	 */
	@PreDestroy
	public void shutdown() {
		logger.info("Arrêt du backend du moteur de recherche intelligent");
	}
}