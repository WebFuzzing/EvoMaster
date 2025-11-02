package com.mongo.persons;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/persons")
public class PersonController {

    @Autowired
    private PersonRepository persons;

    @PostMapping("{age}")
    public ResponseEntity<Void> post(@PathVariable Integer age) {
        Person s = new Person(age);
        persons.save(s);
        return ResponseEntity.status(200).build();
    }

    @GetMapping("list18")
    public ResponseEntity<Void> find18s() {
        int status = (persons.findByAge(18).isEmpty()) ? 400 : 200 ;
        return ResponseEntity.status(status).build();
    }
}


