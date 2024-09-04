package com.mongo.template;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/mongotemplate")
public class MongoTemplateController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("findData")
    public ResponseEntity<Void> findData() {
        Query query = new Query();
        query.addCriteria(Criteria.where("name").ne(null));
        query.addCriteria(Criteria.where("city").ne(null));
        query.addCriteria(Criteria.where("age").lte(18));
        List<MongoTemplateData> results = mongoTemplate.find(query, MongoTemplateData.class, "mongoTemplateDataCollection");
        if (!results.isEmpty()) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(400).build();
        }
    }
}


