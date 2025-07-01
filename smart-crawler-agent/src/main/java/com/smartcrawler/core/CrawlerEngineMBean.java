package com.smartcrawler.core;

/**
 * Interface MBean pour les métriques JMX de CrawlerEngine.
 */
public interface CrawlerEngineMBean {
    long getCrawlCount();
    long getErrorCount();
    long getUsedMemoryMB();
}
