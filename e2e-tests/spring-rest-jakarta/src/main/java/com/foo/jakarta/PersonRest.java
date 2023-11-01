package com.foo.jakarta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(path = "/api/jakarta/person")
public class PersonRest {

    @Autowired
    private PersonRepository repository;


    @RequestMapping(
            method = RequestMethod.POST
    )
    public ResponseEntity<Void> post(String name) {
        PersonEntity entity = new PersonEntity();
        entity.name = name;
        repository.save(entity);
        return ResponseEntity.status(200).build();
    }


    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<Void> get(String name) {

        List<PersonEntity> list = repository.findByName(name);
        if (list.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }
}
