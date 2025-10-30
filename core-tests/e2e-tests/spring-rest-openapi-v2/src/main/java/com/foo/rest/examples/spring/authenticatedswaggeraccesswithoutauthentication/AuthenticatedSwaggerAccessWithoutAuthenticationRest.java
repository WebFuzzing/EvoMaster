package com.foo.rest.examples.spring.authenticatedswaggeraccesswithoutauthentication;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api")
public class AuthenticatedSwaggerAccessWithoutAuthenticationRest {




    @GetMapping(value = "/endpoint1")
    public ResponseEntity getResourceEnd1() {

        return new ResponseEntity<>(HttpStatus.OK);

    }

    @GetMapping(value = "/endpoint2")
    public ResponseEntity getResourceEnd2() {

        return new ResponseEntity<>(HttpStatus.OK);

    }
}