package com.foo.rest.examples.spring.wiremock.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rest.examples.spring.wiremock.service.MockApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
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
public class SearchRest {

    @RequestMapping(
            value = "/search/{key}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public Boolean equalsFoo(@PathVariable("key") String s) {

        boolean responseDto = false;

        try {
            URL url = new URL("http://foo.bar:8080/api/echo/foo");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            MockApiResponse result = mapper.readValue(responseStream, MockApiResponse.class);

            responseDto = result.message.equals("foo");

        } catch (IOException e) {
            e.printStackTrace();
        }


        return responseDto;
    }

}
