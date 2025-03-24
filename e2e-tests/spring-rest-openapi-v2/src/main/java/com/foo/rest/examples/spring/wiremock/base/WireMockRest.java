package com.foo.rest.examples.spring.wiremock.base;

import org.evomaster.client.java.utils.SimpleLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.MediaType;
import java.util.Objects;

@RestController
@RequestMapping(path = "/api/wiremock")
public class WireMockRest {

    @RequestMapping(
            value = "/equalsFoo/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public boolean equalsFoo(
            @PathVariable("s") String s
    ){
        return "foo".equals(s);
    }

    @RequestMapping(
            value = "/external",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public boolean externalCall() {
        boolean responseDto = false;

        RestTemplate restTemplate = new RestTemplate();

        /*
          Below code will call the external api with the value `foo`
          to fetch the response as foo, if it's a success it'll return
          true otherwise false.
          Java DNS cache manipulator will replace the target hostname
          to resolve to localhost, so the WireMock will act as the
          target server.
         */
        String uri = "http://foo.test:8080/api/echo/foo";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (response.getStatusCode().value() == 200 && Objects.requireNonNull(response.getBody()).equals("foo")) {
                responseDto = true;
            }
        } catch (Exception e) {
            SimpleLogger.uniqueWarn(e.getLocalizedMessage());
        }
        return responseDto;
    }
}
