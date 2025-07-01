module com.smartcrawler.smartcrawleragent {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.management;
    requires okhttp3;
    requires org.apache.commons.lang3;
    requires org.jsoup;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.server;
    requires javax.servlet.api;
    requires java.desktop;
    requires org.slf4j;
    requires static lombok;


    opens com.smartcrawler.smartcrawleragent to javafx.fxml;
    exports com.smartcrawler.smartcrawleragent;
    exports com.smartcrawler.utils;


    // Export des classes nécessaires à JavaFX
    exports com.smartcrawler.ui;
    exports com.smartcrawler;
    exports com.smartcrawler.core;

    // 👇 indispensable pour FXML :
    opens com.smartcrawler.ui to javafx.fxml;
    opens com.smartcrawler.core to javafx.fxml;
    opens com.smartcrawler.service to org.eclipse.jetty.server, org.eclipse.jetty.servlet;

    exports com.smartcrawler.service;

    exports com.smartcrawler.model to com.fasterxml.jackson.databind;

    opens com.smartcrawler.model to com.fasterxml.jackson.databind;

    exports com.smartcrawler.model.enums to com.fasterxml.jackson.databind;

}