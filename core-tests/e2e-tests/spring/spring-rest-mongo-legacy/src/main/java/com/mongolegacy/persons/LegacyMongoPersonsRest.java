package com.mongolegacy.persons;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/legacymongo")
public class LegacyMongoPersonsRest {

    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    private MongoCollection<Document> persons() {
        return mongoClient.getDatabase(databaseName).getCollection("persons");
    }

    @PostMapping("/persons")
    public ResponseEntity<Void> create(@RequestBody LegacyPersonDto dto) {
        if (dto == null || dto.name == null || dto.age == null) {
            return ResponseEntity.status(400).build();
        }
        persons().insertOne(new Document("name", dto.name).append("age", dto.age));
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/persons")
    public ResponseEntity<Void> find(@RequestParam String name, @RequestParam int age) {
        /*
            Filters do not return a BsonDocument. With driver 3.x, converting them
            requires Bson.toBsonDocument(Class, CodecRegistry), as the no-arg version only exists since 4.2
         */
        Document found = persons()
                .find(Filters.and(Filters.eq("name", name), Filters.gte("age", age)))
                .first();
        int status = found != null ? 200 : 404;
        return ResponseEntity.status(status).build();
    }
}
