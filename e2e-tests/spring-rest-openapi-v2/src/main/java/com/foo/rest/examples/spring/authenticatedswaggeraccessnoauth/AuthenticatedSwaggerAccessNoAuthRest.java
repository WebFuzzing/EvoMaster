package com.foo.rest.examples.spring.authenticatedswaggeraccessnoauth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api")
public class AuthenticatedSwaggerAccessNoAuthRest {




    @GetMapping(value = "/endpoint1")
    public ResponseEntity getResourceEnd1(@PathVariable("x") String x) {

        return new ResponseEntity<>(x, HttpStatus.OK);

    }

    @GetMapping(value = "/endpoint2")
    public ResponseEntity getResourceEnd2(@PathVariable("x") String x) {

        return new ResponseEntity<>(x + "endpoint2", HttpStatus.OK);

    }
}