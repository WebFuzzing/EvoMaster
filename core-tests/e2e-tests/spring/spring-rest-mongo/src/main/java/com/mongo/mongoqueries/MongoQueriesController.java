package com.mongo.mongoqueries;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(path = "/mongoqueries")
public class MongoQueriesController {

    private static final String COLLECTION_NAME = "mongoQueriesCollection";

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("saveData")
    public ResponseEntity<Void> saveData() {
        mongoTemplate.getCollection(COLLECTION_NAME)
                .createIndex(new Document("location", "2dsphere"));
        mongoTemplate.save(new MongoQueriesData("1", "John", 25, 5L,
                Arrays.asList("a", "b", "c"), "some description",
                geoJsonPoint(2.29441692356368, 48.858504187164684)), COLLECTION_NAME);
        mongoTemplate.save(new MongoQueriesData("2", "Alice", 35, 1L,
                Arrays.asList("a", "b", "d"), "desc",
                geoJsonPoint(-74.044502, 40.689247)), COLLECTION_NAME);
        return ResponseEntity.status(200).build();
    }

    private Document geoJsonPoint(double longitude, double latitude) {
        return new Document("type", "Point")
                .append("coordinates", Arrays.asList(longitude, latitude));
    }

    private Document geoJsonNearQuery(String operator) {
        Document geometry = geoJsonPoint(2.2945, 48.8586);
        return new Document("location",
                new Document(operator,
                        new Document("$geometry", geometry)
                                .append("$maxDistance", 1000.0)));
    }

    private ResponseEntity<Void> executeQuery(Document queryDoc) {
        List<MongoQueriesData> results = mongoTemplate.find(new BasicQuery(queryDoc), MongoQueriesData.class, COLLECTION_NAME);
        if (results != null && !results.isEmpty()) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(400).build();
        }
    }

    @GetMapping("eq")
    public ResponseEntity<Void> findEq() {
        return executeQuery(new Document("age", new Document("$eq", 25)));
    }

    @GetMapping("ne")
    public ResponseEntity<Void> findNe() {
        return executeQuery(new Document("age", new Document("$ne", 25)));
    }

    @GetMapping("lt")
    public ResponseEntity<Void> findLt() {
        return executeQuery(new Document("age", new Document("$lt", 30)));
    }

    @GetMapping("lte")
    public ResponseEntity<Void> findLte() {
        return executeQuery(new Document("age", new Document("$lte", 30)));
    }

    @GetMapping("gt")
    public ResponseEntity<Void> findGt() {
        return executeQuery(new Document("age", new Document("$gt", 18)));
    }

    @GetMapping("gte")
    public ResponseEntity<Void> findGte() {
        return executeQuery(new Document("age", new Document("$gte", 18)));
    }

    @GetMapping("in")
    public ResponseEntity<Void> findIn() {
        return executeQuery(new Document("age", new Document("$in", Arrays.asList(20, 25, 30))));
    }

    @GetMapping("nin")
    public ResponseEntity<Void> findNin() {
        return executeQuery(new Document("age", new Document("$nin", Arrays.asList(10, 20, 30))));
    }

    @GetMapping("mod")
    public ResponseEntity<Void> findMod() {
        return executeQuery(new Document("age", new Document("$mod", Arrays.asList(5L, 0L))));
    }

    @GetMapping("not")
    public ResponseEntity<Void> findNot() {
        return executeQuery(new Document("age", new Document("$not", new Document("$gt", 30))));
    }

    @GetMapping("size")
    public ResponseEntity<Void> findSize() {
        return executeQuery(new Document("tags", new Document("$size", 3)));
    }

    @GetMapping("elemMatch")
    public ResponseEntity<Void> findElemMatch() {
        return executeQuery(new Document("tags", new Document("$elemMatch", new Document("$eq", "b"))));
    }

    @GetMapping("near")
    public ResponseEntity<Void> findNear() {
        return executeQuery(geoJsonNearQuery("$near"));
    }

    @GetMapping("nearSphere")
    public ResponseEntity<Void> findNearSphere() {
        return executeQuery(geoJsonNearQuery("$nearSphere"));
    }

    @GetMapping("bitsAllClear")
    public ResponseEntity<Void> findBitsAllClear() {
        return executeQuery(new Document("flags", new Document("$bitsAllClear", 2L)));
    }

    @GetMapping("bitsAnySet")
    public ResponseEntity<Void> findBitsAnySet() {
        return executeQuery(new Document("flags", new Document("$bitsAnySet", 1L)));
    }

    @GetMapping("bitsAllSet")
    public ResponseEntity<Void> findBitsAllSet() {
        return executeQuery(new Document("flags", new Document("$bitsAllSet", 5L)));
    }

    @GetMapping("bitsAnyClear")
    public ResponseEntity<Void> findBitsAnyClear() {
        return executeQuery(new Document("flags", new Document("$bitsAnyClear", 2L)));
    }

    @GetMapping("all")
    public ResponseEntity<Void> findAll() {
        return executeQuery(new Document("tags", new Document("$all", Arrays.asList("a", "b"))));
    }

    @GetMapping("type")
    public ResponseEntity<Void> findType() {
        return executeQuery(new Document("name", new Document("$type", 2)));
    }

    @GetMapping("exists")
    public ResponseEntity<Void> findExists() {
        return executeQuery(new Document("description", new Document("$exists", true)));
    }

    @GetMapping("nor")
    public ResponseEntity<Void> findNor() {
        return executeQuery(new Document("$nor", Arrays.asList(
                new Document("age", new Document("$gt", 40)),
                new Document("name", new Document("$eq", "wrong"))
        )));
    }

    @GetMapping("or")
    public ResponseEntity<Void> findOr() {
        return executeQuery(new Document("$or", Arrays.asList(
                new Document("age", new Document("$gt", 40)),
                new Document("name", new Document("$eq", "John"))
        )));
    }

    @GetMapping("and")
    public ResponseEntity<Void> findAnd() {
        return executeQuery(new Document("$and", Arrays.asList(
                new Document("age", new Document("$gt", 18)),
                new Document("name", new Document("$eq", "John"))
        )));
    }
}
