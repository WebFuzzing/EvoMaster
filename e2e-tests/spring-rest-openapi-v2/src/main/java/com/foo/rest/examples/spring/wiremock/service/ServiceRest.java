package com.foo.rest.examples.spring.wiremock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rest.examples.spring.strings.StringsResponseDto;
import com.foo.rest.examples.spring.wiremock.http.MockApiResponse;
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
    public StringsResponseDto dummyExternalCall() {
        StringsResponseDto stringsResponseDto = new StringsResponseDto();

        try {
            // Port changed to test the default port scenario respective to the protocol
            URL url = new URL("http://foo.bar/api/echo/foo");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            MockApiResponse result = mapper.readValue(responseStream, MockApiResponse.class);

            if (result.message.equals("foo")) {
                stringsResponseDto.valid = true;
            } else {
                stringsResponseDto.valid = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            stringsResponseDto.valid = false;
        }

        return stringsResponseDto;

    }
}
