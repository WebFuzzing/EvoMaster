package com.mongo.count;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Counts documents directly through the Mongo driver (MongoCollection.countDocuments),
 * not through Spring Data, so the filter is only visible at the driver level.
 * There is one endpoint for each overload of countDocuments.
 */
@RestController
@RequestMapping(path = "/mongocount")
public class MongoCountController {

    /*
        Each endpoint counts on its own collection, so that a document that
        makes one endpoint return 200 cannot accidentally do it for another one.
     */
    private static final String COUNT_ALL = "countAllCollection";
    private static final String COUNT_BSON = "countBsonCollection";
    private static final String COUNT_BSON_OPTIONS = "countBsonOptionsCollection";
    private static final String COUNT_SESSION = "countSessionCollection";
    private static final String COUNT_SESSION_BSON = "countSessionBsonCollection";
    private static final String COUNT_SESSION_BSON_OPTIONS = "countSessionBsonOptionsCollection";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    private MongoCollection<MongoCountData> collection(String collectionName) {
        return mongoTemplate.getDb().getCollection(collectionName, MongoCountData.class);
    }

    private static Bson filter(String city, int minAge) {
        return Filters.and(Filters.eq("city", city), Filters.gte("age", minAge));
    }

    private static ResponseEntity<Void> response(long count) {
        return ResponseEntity.status(count > 0 ? 200 : 404).build();
    }

    /**
     * countDocuments()
     */
    @GetMapping("countAll")
    public ResponseEntity<Void> countAll() {
        return response(collection(COUNT_ALL).countDocuments());
    }

    /**
     * countDocuments(Bson)
     */
    @GetMapping("countByCityAndAge")
    public ResponseEntity<Void> countByCityAndAge(@RequestParam(name = "city") String city,
                                                  @RequestParam(name = "minAge") int minAge) {
        return response(collection(COUNT_BSON).countDocuments(filter(city, minAge)));
    }

    /**
     * countDocuments(Bson, CountOptions)
     */
    @GetMapping("countByCityAndAgeWithOptions")
    public ResponseEntity<Void> countByCityAndAgeWithOptions(@RequestParam(name = "city") String city,
                                                             @RequestParam(name = "minAge") int minAge) {
        return response(collection(COUNT_BSON_OPTIONS).countDocuments(filter(city, minAge), new CountOptions().limit(1)));
    }

    /**
     * countDocuments(ClientSession)
     */
    @GetMapping("countAllInSession")
    public ResponseEntity<Void> countAllInSession() {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(COUNT_SESSION).countDocuments(session));
        }
    }

    /**
     * countDocuments(ClientSession, Bson)
     */
    @GetMapping("countByCityAndAgeInSession")
    public ResponseEntity<Void> countByCityAndAgeInSession(@RequestParam(name = "city") String city,
                                                           @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(COUNT_SESSION_BSON).countDocuments(session, filter(city, minAge)));
        }
    }

    /**
     * countDocuments(ClientSession, Bson, CountOptions)
     */
    @GetMapping("countByCityAndAgeInSessionWithOptions")
    public ResponseEntity<Void> countByCityAndAgeInSessionWithOptions(@RequestParam(name = "city") String city,
                                                                      @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(COUNT_SESSION_BSON_OPTIONS).countDocuments(session, filter(city, minAge), new CountOptions().limit(1)));
        }
    }
}
