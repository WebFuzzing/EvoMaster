package com.mongo.objectid;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/objectid")
public class MongoObjectIdController {

    @GetMapping("createObjectId")
    public ResponseEntity<Void> createObjectId(String string) {
        int status;
        try {
            new ObjectId(string);
            status = 200;
        } catch (IllegalArgumentException ex) {
            status = 400;
        }
        return ResponseEntity.status(status).build();
    }
}


