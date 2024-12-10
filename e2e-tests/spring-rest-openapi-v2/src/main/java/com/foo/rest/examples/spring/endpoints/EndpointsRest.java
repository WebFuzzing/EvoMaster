package com.foo.rest.examples.spring.endpoints;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/endpoints")
public class EndpointsRest {

    @RequestMapping(value = "/options", method = RequestMethod.OPTIONS)
    ResponseEntity<?> options() {
        return ResponseEntity
                .ok()
                .allow(HttpMethod.OPTIONS)
                .build();
    }

    @RequestMapping(value = "/trace", method = RequestMethod.TRACE)
    ResponseEntity<?> trace() {
        return ResponseEntity
                .ok()
                .allow(HttpMethod.TRACE)
                .build();
    }

    @RequestMapping(value = "/head", method = RequestMethod.HEAD)
    ResponseEntity<?> head() {
        return ResponseEntity
                .ok()
                .allow(HttpMethod.HEAD)
                .build();
    }

}
