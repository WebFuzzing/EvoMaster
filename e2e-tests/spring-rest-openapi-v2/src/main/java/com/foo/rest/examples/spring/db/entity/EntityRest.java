package com.foo.rest.examples.spring.db.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(path = "/api/db/entity")
public class EntityRest {

    @Autowired
    private EntityRepository repository;


    @RequestMapping(
            method = RequestMethod.PUT
    )
    public ResponseEntity put() {

        repository.findById(0); // FIXME currently not handling empty SELECTs with no WHERE
        EntityData data = repository.findAll().iterator().next();

        data.setX1(42);

        repository.save(data); // this should fail if any value is null

        return ResponseEntity.ok("OK");
    }
}
