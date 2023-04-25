package com.foo.rest.examples.spring.db.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/db/jpa")
public class EntityJPARest {

    @Autowired
    private EntityJPARepository repository;


    @RequestMapping(
            method = RequestMethod.PUT
    )
    public ResponseEntity put() {

        repository.findById(0); // FIXME currently not handling empty SELECTs with no WHERE
        EntityJPAData data = repository.findAll().iterator().next();

        data.setX1(42);

        repository.save(data); // this should fail if any value is invalid

        return ResponseEntity.ok("OK");
    }
}
