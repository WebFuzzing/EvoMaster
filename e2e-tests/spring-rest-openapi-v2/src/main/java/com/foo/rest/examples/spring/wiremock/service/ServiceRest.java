package com.foo.rest.examples.spring.wiremock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rest.examples.spring.wiremock.base.ResponseDto;
import org.evomaster.client.java.utils.SimpleLogger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ResponseDto> dummyExternalCall() {
        ResponseDto responseDto = new ResponseDto();

        int responseCode = -1;

        try {
            // To bind WireMock in port 80 and 443 require root privileges
            // To avoid that port set to 3000 for e2e-test
            URL url = new URL("http://foo.bar:8080/api/echo/foo");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            responseCode = connection.getResponseCode();
            responseDto.valid = connection.getResponseCode() == 200;
        } catch (IOException e) {
            SimpleLogger.uniqueWarn(e.getLocalizedMessage());
        }

        // Note: Response will not be useful at this, since there is no mutation happening
        // inside ExternalServiceAction.

        switch (responseCode) {
            case(200):
                return new ResponseEntity<>(responseDto, HttpStatus.OK);
            case(400):
                return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
            case(401):
                return new ResponseEntity<>(responseDto, HttpStatus.UNAUTHORIZED);
            case(403):
                return new ResponseEntity<>(responseDto, HttpStatus.FORBIDDEN);
            case(404):
                return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
            default:
                return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * To test multiple external calls under a single REST call
     */

//    @RequestMapping(
//            value = "/external/complex",
//            method = RequestMethod.GET,
//            produces = MediaType.APPLICATION_JSON
//    )
    public ResponseEntity<ResponseDto> complexExternalCalls() {
        ResponseDto responseDto = new ResponseDto();

        int responseCode = -1;

        try {
            // To bind WireMock in port 80 and 443 require root privileges
            // To avoid that port set to 3000 for e2e-test
            URL url = new URL("http://foo.baz:8080/api/echo/bar");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            int r = connection.getResponseCode();

            if (r == 200) {
                URL secondURL = new URL("http://bar.baz:8080/api/echo/bazz");
                HttpURLConnection secondConnection = (HttpURLConnection) secondURL.openConnection();
                secondConnection.setRequestProperty("accept", "application/json");

                responseCode = secondConnection.getResponseCode();
                responseDto.valid = secondConnection.getResponseCode() == 200;
            }
        } catch (IOException e) {
            SimpleLogger.uniqueWarn(e.getLocalizedMessage());
        }

        // Note: Response will not be useful at this, since there is no mutation happening
        // inside ExternalServiceAction.

        switch (responseCode) {
            case(200):
                return new ResponseEntity<>(responseDto, HttpStatus.OK);
            case(400):
                return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
            case(401):
                return new ResponseEntity<>(responseDto, HttpStatus.UNAUTHORIZED);
            case(403):
                return new ResponseEntity<>(responseDto, HttpStatus.FORBIDDEN);
            case(404):
                return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
            default:
                return new ResponseEntity<>(responseDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * An endpoint to receive JSON response from external service and response
     * true or false based on the result.
     */
//    @RequestMapping(
//            value = "/external/json",
//            method = RequestMethod.GET,
//            produces = MediaType.APPLICATION_JSON
//    )
    public ResponseDto jsonResponse() {
        ResponseDto responseDto = new ResponseDto();

        try {
            // To bind WireMock in port 80 and 443 require root privileges
            // To avoid that port set to 3000 for e2e-test
            URL url = new URL("http://foo.bar:8080/api/echo/foo/json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            MockApiResponse result = mapper.readValue(responseStream, MockApiResponse.class);

            responseDto.valid = result.message.equals("foo");
        } catch (IOException e) {
            SimpleLogger.uniqueWarn(e.getLocalizedMessage());
        }

        return responseDto;

    }

}