package com.foo.rest.examples.spring.wiremock.http;

import com.foo.rest.examples.spring.strings.StringsResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/wiremock")
public class HttpRequestRest {

    @RequestMapping(
            value = "/external",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto externalCall() {
        StringsResponseDto stringsResponseDto = new StringsResponseDto();

        RestTemplate restTemplate = new RestTemplate();

        String uri = "http://foo.bar:8080/api/echo/foo";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (response.getStatusCode().value() == 200 && response.getBody().equals("foo")) {
                stringsResponseDto.valid = true;
            } else {
                stringsResponseDto.valid = false;
            }
        } catch (Exception e) {
            stringsResponseDto.valid = false;
        }
        return stringsResponseDto;
    }
}
