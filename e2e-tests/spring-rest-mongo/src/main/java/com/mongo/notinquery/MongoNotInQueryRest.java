package com.mongo.notinquery;

import com.mongo.findstring.MongoFindStringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "/notinquery")
public class MongoNotInQueryRest {

    @Autowired
    private MongoNotInQueryRepository repository;

    @GetMapping("/{id}")
    public ResponseEntity<Void> find(@PathVariable String id) {

        if(id == null || id.length() < 2){
            return ResponseEntity.status(400).build();
        }

        MongoNotInQueryData data = repository.findById(id).orElse(null);
        int status = 404;
        if(data != null){
            status = 200;
            if(data.getX() != null && data.getX() > 42){
                status = 201;
            }
        }

        return ResponseEntity.status(status).build();
    }

}
