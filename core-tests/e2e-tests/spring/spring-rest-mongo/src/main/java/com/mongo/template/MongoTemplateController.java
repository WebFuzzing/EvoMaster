package com.mongo.template;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/mongotemplate")
public class MongoTemplateController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("saveData")
    public ResponseEntity<Void> saveData() {
        mongoTemplate.save(new MongoTemplateData("myData"), "mongoTemplateDataCollection");
        return ResponseEntity.status(200).build();
    }

    @GetMapping("findData")
    public ResponseEntity<Void> findData() {
        MongoTemplateData rv = mongoTemplate.findOne(new Query(), MongoTemplateData.class, "mongoTemplateDataCollection");
        if (rv != null) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(400).build();
        }
    }
}


