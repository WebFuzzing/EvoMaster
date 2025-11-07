package com.foo.rest.examples.spring.redirect;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
@RestController
@RequestMapping(path = "/api/redirect")
public class RedirectRest {


    @GetMapping(path = "/301")
    public ResponseEntity get301(){
        return ResponseEntity.status(301).location(URI.create("/api/redirect/foo")).body("301");
    }

    @GetMapping(path = "/302")
    public ResponseEntity get302(){
        return ResponseEntity.status(302).location(URI.create("/api/redirect/foo")).body("302");
    }

    @GetMapping(path = "/307")
    public ResponseEntity get307(){
        return ResponseEntity.status(307).location(URI.create("/api/redirect/foo")).body("307");
    }

    @GetMapping(path = "/308")
    public ResponseEntity get308(){
        return ResponseEntity.status(308).location(URI.create("/api/redirect/foo")).body("308");
    }

    @GetMapping(path = "/foo")
    public ResponseEntity getFoo(){
        return ResponseEntity.status(200).body("foo");
    }


}
