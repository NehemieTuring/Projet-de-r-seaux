package com.smartcrawler.utils;

/**
 * Interface pour le logging structuré.
 */
public interface Logger {
    void info(String message, Throwable throwable);
    void warn(String message, Throwable throwable);
    void error(String message, Throwable throwable);
    void debug(String message, Throwable throwable);
}