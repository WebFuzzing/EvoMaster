package com.foo.rest.examples.spring.db.javatypes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(path = "/api/db/javatypes")
public class JavaTypesRest {

    @Autowired
    private JavaTypesRepository repository;


    @RequestMapping(
            method = RequestMethod.POST
    )
    public void post() {
        JavaTypesEntity entity = new JavaTypesEntity();
        repository.save(entity);
    }


    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity get() {

        if (repository.findAll().iterator().hasNext()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }
}
