package com.foo.rest.examples.spring.wiremock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rest.examples.spring.wiremock.http.MockApiResponse;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class ServiceApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) {
        try {
            URL url = new URL("http://baz.bar:8080/api/echo/foo");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            MockApiResponse result = mapper.readValue(responseStream, MockApiResponse.class);

            if (result.message.equals("foo")) {
                System.out.println("Call success");
            } else {
                System.out.println("Call failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
