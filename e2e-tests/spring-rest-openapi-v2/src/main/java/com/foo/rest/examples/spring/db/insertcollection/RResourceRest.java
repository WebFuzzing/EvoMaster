package com.foo.rest.examples.spring.db.insertcollection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

/**
 * created by manzhang on 2021/11/10
 */
@RestController
@RequestMapping(path = "/api/db/insertcollection")
public class RResourceRest {

    @Autowired
    private RResourceRepository repository;

    @RequestMapping(
            path = "",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity getAll() {
        if (repository.count() > 20)
            return ResponseEntity.status(200).build();
        return ResponseEntity.status(400).build();
    }
}
