package com.mongo.delete;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deletes documents directly through the Mongo driver (MongoCollection.deleteOne/deleteMany),
 * not through Spring Data, so the filter is only visible at the driver level.
 * There is one endpoint for each overload of deleteOne and deleteMany.
 */
@RestController
@RequestMapping(path = "/mongodelete")
public class MongoDeleteController {

    /*
        Each endpoint deletes from its own collection, so that a document that
        makes one endpoint return 200 cannot accidentally do it for another one.
     */
    private static final String DELETE_ONE_BY_CITY_AND_AGE = "deleteOneByCityAndAgeCollection";
    private static final String DELETE_ONE_BY_CITY_AND_AGE_WITH_OPTIONS = "deleteOneByCityAndAgeWithOptionsCollection";
    private static final String DELETE_ONE_BY_CITY_AND_AGE_IN_SESSION = "deleteOneByCityAndAgeInSessionCollection";
    private static final String DELETE_ONE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS = "deleteOneByCityAndAgeInSessionWithOptionsCollection";
    private static final String DELETE_MANY_BY_CITY_AND_AGE = "deleteManyByCityAndAgeCollection";
    private static final String DELETE_MANY_BY_CITY_AND_AGE_WITH_OPTIONS = "deleteManyByCityAndAgeWithOptionsCollection";
    private static final String DELETE_MANY_BY_CITY_AND_AGE_IN_SESSION = "deleteManyByCityAndAgeInSessionCollection";
    private static final String DELETE_MANY_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS = "deleteManyByCityAndAgeInSessionWithOptionsCollection";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    private MongoCollection<MongoDeleteData> collection(String collectionName) {
        return mongoTemplate.getDb().getCollection(collectionName, MongoDeleteData.class);
    }

    private static Bson filter(String city, int minAge) {
        return Filters.and(Filters.eq("city", city), Filters.gte("age", minAge));
    }

    private static DeleteOptions options() {
        return new DeleteOptions().collation(Collation.builder().locale("en").build());
    }

    private static ResponseEntity<Void> response(DeleteResult result) {
        return ResponseEntity.status(result.getDeletedCount() > 0 ? 200 : 404).build();
    }

    /**
     * deleteOne(Bson)
     */
    @DeleteMapping("deleteOneByCityAndAge")
    public ResponseEntity<Void> deleteOneByCityAndAge(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(DELETE_ONE_BY_CITY_AND_AGE).deleteOne(filter(city, minAge)));
    }

    /**
     * deleteOne(Bson, DeleteOptions)
     */
    @DeleteMapping("deleteOneByCityAndAgeWithOptions")
    public ResponseEntity<Void> deleteOneByCityAndAgeWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(DELETE_ONE_BY_CITY_AND_AGE_WITH_OPTIONS).deleteOne(filter(city, minAge), options()));
    }

    /**
     * deleteOne(ClientSession, Bson)
     */
    @DeleteMapping("deleteOneByCityAndAgeInSession")
    public ResponseEntity<Void> deleteOneByCityAndAgeInSession(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(DELETE_ONE_BY_CITY_AND_AGE_IN_SESSION).deleteOne(session, filter(city, minAge)));
        }
    }

    /**
     * deleteOne(ClientSession, Bson, DeleteOptions)
     */
    @DeleteMapping("deleteOneByCityAndAgeInSessionWithOptions")
    public ResponseEntity<Void> deleteOneByCityAndAgeInSessionWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(DELETE_ONE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS).deleteOne(session, filter(city, minAge), options()));
        }
    }

    /**
     * deleteMany(Bson)
     */
    @DeleteMapping("deleteManyByCityAndAge")
    public ResponseEntity<Void> deleteManyByCityAndAge(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(DELETE_MANY_BY_CITY_AND_AGE).deleteMany(filter(city, minAge)));
    }

    /**
     * deleteMany(Bson, DeleteOptions)
     */
    @DeleteMapping("deleteManyByCityAndAgeWithOptions")
    public ResponseEntity<Void> deleteManyByCityAndAgeWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(DELETE_MANY_BY_CITY_AND_AGE_WITH_OPTIONS).deleteMany(filter(city, minAge), options()));
    }

    /**
     * deleteMany(ClientSession, Bson)
     */
    @DeleteMapping("deleteManyByCityAndAgeInSession")
    public ResponseEntity<Void> deleteManyByCityAndAgeInSession(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(DELETE_MANY_BY_CITY_AND_AGE_IN_SESSION).deleteMany(session, filter(city, minAge)));
        }
    }

    /**
     * deleteMany(ClientSession, Bson, DeleteOptions)
     */
    @DeleteMapping("deleteManyByCityAndAgeInSessionWithOptions")
    public ResponseEntity<Void> deleteManyByCityAndAgeInSessionWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(DELETE_MANY_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS).deleteMany(session, filter(city, minAge), options()));
        }
    }
}
