package com.foo.rest.examples.spring.wiremock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping(path = "/api/wiremock")
public class ServiceRest {

    @RequestMapping(
            value = "/external",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public boolean dummyExternalCall() {
        boolean responseDto = false;

        try {
            // To bind WireMock in port 80 and 443 require root privileges
            // To avoid that port set to 3000 for e2etest
            URL url = new URL("http://foo.bar:8080/api/echo/foo");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            MockApiResponse result = mapper.readValue(responseStream, MockApiResponse.class);

            if (result.message.equals("foo")) {
                responseDto = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseDto;
    }

}