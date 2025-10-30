package com.mongo.findstring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(path = "/findstring")
public class MongoFindStringRest {

    @Autowired
    private MongoFindStringRepository repository;

    @GetMapping("/{x}")
    public ResponseEntity<Void> find(@PathVariable String x) {

        if(x == null || x.length() < 2){
            return ResponseEntity.status(400).build();
        }

        int status = (repository.findById(x).isPresent()) ? 200 : 404 ;
        return ResponseEntity.status(status).build();
    }

}
