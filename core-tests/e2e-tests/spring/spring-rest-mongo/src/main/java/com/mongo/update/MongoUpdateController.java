package com.mongo.update;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * Updates documents directly through the Mongo driver (MongoCollection.updateOne/updateMany),
 * not through Spring Data, so the filter is only visible at the driver level.
 * There is one endpoint for each overload of updateOne and updateMany.
 */
@RestController
@RequestMapping(path = "/mongoupdate")
public class MongoUpdateController {

    /*
        Each endpoint updates its own collection, so that a document that
        makes one endpoint return 200 cannot accidentally do it for another one.
     */
    private static final String UPDATE_ONE_BY_CITY_AND_AGE = "updateOneByCityAndAgeCollection";
    private static final String UPDATE_ONE_BY_CITY_AND_AGE_WITH_OPTIONS = "updateOneByCityAndAgeWithOptionsCollection";
    private static final String UPDATE_ONE_BY_CITY_AND_AGE_IN_SESSION = "updateOneByCityAndAgeInSessionCollection";
    private static final String UPDATE_ONE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS = "updateOneByCityAndAgeInSessionWithOptionsCollection";
    private static final String UPDATE_ONE_BY_CITY_AND_AGE_WITH_PIPELINE = "updateOneByCityAndAgeWithPipelineCollection";
    private static final String UPDATE_ONE_BY_CITY_AND_AGE_WITH_PIPELINE_AND_OPTIONS = "updateOneByCityAndAgeWithPipelineAndOptionsCollection";
    private static final String UPDATE_ONE_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE = "updateOneByCityAndAgeInSessionWithPipelineCollection";
    private static final String UPDATE_ONE_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE_AND_OPTIONS = "updateOneByCityAndAgeInSessionWithPipelineAndOptionsCollection";
    private static final String UPDATE_MANY_BY_CITY_AND_AGE = "updateManyByCityAndAgeCollection";
    private static final String UPDATE_MANY_BY_CITY_AND_AGE_WITH_OPTIONS = "updateManyByCityAndAgeWithOptionsCollection";
    private static final String UPDATE_MANY_BY_CITY_AND_AGE_IN_SESSION = "updateManyByCityAndAgeInSessionCollection";
    private static final String UPDATE_MANY_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS = "updateManyByCityAndAgeInSessionWithOptionsCollection";
    private static final String UPDATE_MANY_BY_CITY_AND_AGE_WITH_PIPELINE = "updateManyByCityAndAgeWithPipelineCollection";
    private static final String UPDATE_MANY_BY_CITY_AND_AGE_WITH_PIPELINE_AND_OPTIONS = "updateManyByCityAndAgeWithPipelineAndOptionsCollection";
    private static final String UPDATE_MANY_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE = "updateManyByCityAndAgeInSessionWithPipelineCollection";
    private static final String UPDATE_MANY_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE_AND_OPTIONS = "updateManyByCityAndAgeInSessionWithPipelineAndOptionsCollection";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    private MongoCollection<MongoUpdateData> collection(String collectionName) {
        return mongoTemplate.getDb().getCollection(collectionName, MongoUpdateData.class);
    }

    private static Bson filter(String city, int minAge) {
        return Filters.and(Filters.eq("city", city), Filters.gte("age", minAge));
    }

    private static Bson update() {
        return Updates.set("verified", true);
    }

    private static List<Bson> pipeline() {
        return Collections.singletonList(new Document("$set", new Document("verified", true)));
    }

    private static UpdateOptions options() {
        return new UpdateOptions().bypassDocumentValidation(true);
    }

    /*
        matched (and not modified) count is used, as an existing document might already be verified
     */
    private static ResponseEntity<Void> response(UpdateResult result) {
        return ResponseEntity.status(result.getMatchedCount() > 0 ? 200 : 404).build();
    }

    /**
     * updateOne(Bson, Bson)
     */
    @PutMapping("updateOneByCityAndAge")
    public ResponseEntity<Void> updateOneByCityAndAge(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(UPDATE_ONE_BY_CITY_AND_AGE).updateOne(filter(city, minAge), update()));
    }

    /**
     * updateOne(Bson, Bson, UpdateOptions)
     */
    @PutMapping("updateOneByCityAndAgeWithOptions")
    public ResponseEntity<Void> updateOneByCityAndAgeWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(UPDATE_ONE_BY_CITY_AND_AGE_WITH_OPTIONS).updateOne(filter(city, minAge), update(), options()));
    }

    /**
     * updateOne(ClientSession, Bson, Bson)
     */
    @PutMapping("updateOneByCityAndAgeInSession")
    public ResponseEntity<Void> updateOneByCityAndAgeInSession(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(UPDATE_ONE_BY_CITY_AND_AGE_IN_SESSION).updateOne(session, filter(city, minAge), update()));
        }
    }

    /**
     * updateOne(ClientSession, Bson, Bson, UpdateOptions)
     */
    @PutMapping("updateOneByCityAndAgeInSessionWithOptions")
    public ResponseEntity<Void> updateOneByCityAndAgeInSessionWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(UPDATE_ONE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS).updateOne(session, filter(city, minAge), update(), options()));
        }
    }

    /**
     * updateOne(Bson, List<? extends Bson>)
     */
    @PutMapping("updateOneByCityAndAgeWithPipeline")
    public ResponseEntity<Void> updateOneByCityAndAgeWithPipeline(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(UPDATE_ONE_BY_CITY_AND_AGE_WITH_PIPELINE).updateOne(filter(city, minAge), pipeline()));
    }

    /**
     * updateOne(Bson, List<? extends Bson>, UpdateOptions)
     */
    @PutMapping("updateOneByCityAndAgeWithPipelineAndOptions")
    public ResponseEntity<Void> updateOneByCityAndAgeWithPipelineAndOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(UPDATE_ONE_BY_CITY_AND_AGE_WITH_PIPELINE_AND_OPTIONS).updateOne(filter(city, minAge), pipeline(), options()));
    }

    /**
     * updateOne(ClientSession, Bson, List<? extends Bson>)
     */
    @PutMapping("updateOneByCityAndAgeInSessionWithPipeline")
    public ResponseEntity<Void> updateOneByCityAndAgeInSessionWithPipeline(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(UPDATE_ONE_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE).updateOne(session, filter(city, minAge), pipeline()));
        }
    }

    /**
     * updateOne(ClientSession, Bson, List<? extends Bson>, UpdateOptions)
     */
    @PutMapping("updateOneByCityAndAgeInSessionWithPipelineAndOptions")
    public ResponseEntity<Void> updateOneByCityAndAgeInSessionWithPipelineAndOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(UPDATE_ONE_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE_AND_OPTIONS).updateOne(session, filter(city, minAge), pipeline(), options()));
        }
    }

    /**
     * updateMany(Bson, Bson)
     */
    @PutMapping("updateManyByCityAndAge")
    public ResponseEntity<Void> updateManyByCityAndAge(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(UPDATE_MANY_BY_CITY_AND_AGE).updateMany(filter(city, minAge), update()));
    }

    /**
     * updateMany(Bson, Bson, UpdateOptions)
     */
    @PutMapping("updateManyByCityAndAgeWithOptions")
    public ResponseEntity<Void> updateManyByCityAndAgeWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(UPDATE_MANY_BY_CITY_AND_AGE_WITH_OPTIONS).updateMany(filter(city, minAge), update(), options()));
    }

    /**
     * updateMany(ClientSession, Bson, Bson)
     */
    @PutMapping("updateManyByCityAndAgeInSession")
    public ResponseEntity<Void> updateManyByCityAndAgeInSession(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(UPDATE_MANY_BY_CITY_AND_AGE_IN_SESSION).updateMany(session, filter(city, minAge), update()));
        }
    }

    /**
     * updateMany(ClientSession, Bson, Bson, UpdateOptions)
     */
    @PutMapping("updateManyByCityAndAgeInSessionWithOptions")
    public ResponseEntity<Void> updateManyByCityAndAgeInSessionWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(UPDATE_MANY_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS).updateMany(session, filter(city, minAge), update(), options()));
        }
    }

    /**
     * updateMany(Bson, List<? extends Bson>)
     */
    @PutMapping("updateManyByCityAndAgeWithPipeline")
    public ResponseEntity<Void> updateManyByCityAndAgeWithPipeline(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(UPDATE_MANY_BY_CITY_AND_AGE_WITH_PIPELINE).updateMany(filter(city, minAge), pipeline()));
    }

    /**
     * updateMany(Bson, List<? extends Bson>, UpdateOptions)
     */
    @PutMapping("updateManyByCityAndAgeWithPipelineAndOptions")
    public ResponseEntity<Void> updateManyByCityAndAgeWithPipelineAndOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(UPDATE_MANY_BY_CITY_AND_AGE_WITH_PIPELINE_AND_OPTIONS).updateMany(filter(city, minAge), pipeline(), options()));
    }

    /**
     * updateMany(ClientSession, Bson, List<? extends Bson>)
     */
    @PutMapping("updateManyByCityAndAgeInSessionWithPipeline")
    public ResponseEntity<Void> updateManyByCityAndAgeInSessionWithPipeline(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(UPDATE_MANY_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE).updateMany(session, filter(city, minAge), pipeline()));
        }
    }

    /**
     * updateMany(ClientSession, Bson, List<? extends Bson>, UpdateOptions)
     */
    @PutMapping("updateManyByCityAndAgeInSessionWithPipelineAndOptions")
    public ResponseEntity<Void> updateManyByCityAndAgeInSessionWithPipelineAndOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(UPDATE_MANY_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE_AND_OPTIONS).updateMany(session, filter(city, minAge), pipeline(), options()));
        }
    }
}
