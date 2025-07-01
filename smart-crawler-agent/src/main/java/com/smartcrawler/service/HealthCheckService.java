package com.smartcrawler.service;

import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Service exposant un endpoint HTTP /health pour vérifier l'état de l'application.
 */
public class HealthCheckService {

    private final Logger logger;
    private final ConfigManager configManager;
    private final Server server;

    public HealthCheckService() {
        this.logger = LoggerFactory.getLogger(HealthCheckService.class.getName());
        this.configManager = ConfigManager.getInstance();
        int port = configManager.getIntProperty("healthcheck.port", 8081);
        this.server = new Server(port);
        setupServer();
    }

    private void setupServer() {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ServletHolder healthServlet = context.addServlet(HealthServlet.class, "/health");
        healthServlet.setInitOrder(1);
    }

    /**
     * Démarre le serveur de health check.
     */
    public void start() {
        try {
            server.start();
            logger.info("Health check server started on port: " + server.getURI().getPort(), null);
        } catch (Exception e) {
            logger.error("Error starting health check server", e);
        }
    }

    /**
     * Arrête le serveur de health check.
     */
    public void stop() {
        try {
            server.stop();
            logger.info("Health check server stopped", null);
        } catch (Exception e) {
            logger.error("Error stopping health check server", e);
        }
    }

    /**
     * Servlet pour l'endpoint /health.
     */
    public static class HealthServlet extends HttpServlet {
        private final Logger logger = LoggerFactory.getLogger(HealthServlet.class.getName());

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            String status = "{\"status\":\"UP\",\"timestamp\":" + System.currentTimeMillis() + "}";
            resp.getWriter().write(status);
            logger.debug("Health check requested, status: UP", null);
        }
    }
}