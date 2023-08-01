package com.mongo.personswithoutpost;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/persons")
public class PersonWithoutPostController {

    @Autowired
    private PersonRepository persons;

    @GetMapping("list18")
    public ResponseEntity<Void> find18s() {
        int status = (persons.findByAge(18).isEmpty()) ? 400 : 200 ;
        return ResponseEntity.status(status).build();
    }
}


