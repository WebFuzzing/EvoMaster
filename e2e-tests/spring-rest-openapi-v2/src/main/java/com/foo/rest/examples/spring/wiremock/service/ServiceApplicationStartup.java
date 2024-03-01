package com.foo.rest.examples.spring.wiremock.service;

import org.evomaster.client.java.utils.SimpleLogger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class ServiceApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) {
        try {
            // To bind WireMock in port 80 and 443 require root privileges
            // To avoid that port set to 3000 for e2etest
            URL url = new URL("https://foobarbazz.com:8443/api/echo/foo");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(500); // added to reduce time during testing
            connection.setRequestProperty("accept", "application/json");

            if (connection.getResponseCode() == 200) {
                SimpleLogger.info("External service call at start-up is a success");
            } else {
                SimpleLogger.info("External service call at start-up is a failure");
            }
        } catch (IOException e) {
            SimpleLogger.uniqueWarn(e.getLocalizedMessage());
        }
    }
}
