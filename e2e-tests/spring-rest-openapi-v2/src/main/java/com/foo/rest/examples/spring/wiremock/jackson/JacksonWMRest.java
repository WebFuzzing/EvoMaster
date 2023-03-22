package com.foo.rest.examples.spring.wiremock.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rest.examples.spring.wiremock.base.ResponseDto;
import org.evomaster.client.java.utils.SimpleLogger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping(path = "/api/jackson")
public class JacksonWMRest {


    /**
     * An endpoint to receive JSON response from external service and response
     * true or false based on the result.
     */
    @RequestMapping(
            value = "/json",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<ResponseDto> jsonResponse() {
        ResponseDto responseDto = new ResponseDto();
        responseDto.valid = false;

        try {
            // To bind WireMock in port 80 and 443 require root privileges
            // To avoid that port set to 3000 for e2e-test
            URL url = new URL("http://foo.bar:8080/api/echo/foo/json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            DummyResponse response = mapper.readValue(responseStream, DummyResponse.class);

            if (response.message != null && response.message.equals("foo")) {
                responseDto.valid = true;
                return new ResponseEntity<>(responseDto, HttpStatus.OK);
            }
        } catch (IOException e) {
            SimpleLogger.uniqueWarn(e.getLocalizedMessage());
            return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }


//    @RequestMapping(
//            value = "/url",
//            method = RequestMethod.GET,
//            produces = MediaType.APPLICATION_JSON
//    )
    public ResponseEntity<ResponseDto> experimentalJSONFromURL() {
        ResponseDto responseDto = new ResponseDto();
        responseDto.valid = false;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            URL url = new URL("http://foo.bar:8080/api/echo/foo/json");

            DummyResponse response = objectMapper.readValue(url, DummyResponse.class);
            if (response.message != null && response.message.equals("foo")) {
                responseDto.valid = true;
                return new ResponseEntity<>(responseDto, HttpStatus.OK);
            }
        } catch (IOException e) {
            return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);

    }

//    @RequestMapping(
//            value = "/byte/{s}",
//            method = RequestMethod.GET,
//            produces = MediaType.APPLICATION_JSON
//    )
    public ResponseEntity<ResponseDto> jsonFromByteArray(@PathVariable("s") String s) {
        ResponseDto responseDto = new ResponseDto();
        responseDto.valid = false;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String sampleJSON = String.format("{\"message\":\"%s\"}", s);

            DummyResponse response = objectMapper.readValue(sampleJSON.getBytes(), DummyResponse.class);

            if (response.message != null && response.message.equals("foo")) {
                responseDto.valid = true;
                return new ResponseEntity<>(responseDto, HttpStatus.OK);
            }
        } catch (IOException e) {
            return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
