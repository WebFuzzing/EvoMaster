package com.mongo.findoneandupdate;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
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
 * Updates documents directly through the Mongo driver (MongoCollection.findOneAndUpdate),
 * not through Spring Data, so the filter is only visible at the driver level.
 * There is one endpoint for each overload of findOneAndUpdate.
 */
@RestController
@RequestMapping(path = "/mongofindoneandupdate")
public class MongoFindOneAndUpdateController {

    /*
        Each endpoint uses its own collection, so that a document that
        makes one endpoint return 200 cannot accidentally do it for another one.
     */
    private static final String FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE = "findOneAndUpdateByCityAndAgeCollection";
    private static final String FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_WITH_OPTIONS = "findOneAndUpdateByCityAndAgeWithOptionsCollection";
    private static final String FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_IN_SESSION = "findOneAndUpdateByCityAndAgeInSessionCollection";
    private static final String FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS = "findOneAndUpdateByCityAndAgeInSessionWithOptionsCollection";
    private static final String FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_WITH_PIPELINE = "findOneAndUpdateByCityAndAgeWithPipelineCollection";
    private static final String FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_WITH_PIPELINE_AND_OPTIONS = "findOneAndUpdateByCityAndAgeWithPipelineAndOptionsCollection";
    private static final String FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE = "findOneAndUpdateByCityAndAgeInSessionWithPipelineCollection";
    private static final String FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE_AND_OPTIONS = "findOneAndUpdateByCityAndAgeInSessionWithPipelineAndOptionsCollection";

    /*
        findOneAndUpdate returns the document, so (unlike count, delete or update)
        the driver needs a codec to decode it into our own class
     */
    private static final CodecRegistry POJO_CODEC_REGISTRY = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    private MongoCollection<MongoFindOneAndUpdateData> collection(String collectionName) {
        return mongoTemplate.getDb()
                .getCollection(collectionName, MongoFindOneAndUpdateData.class)
                .withCodecRegistry(POJO_CODEC_REGISTRY);
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

    private static FindOneAndUpdateOptions options() {
        return new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
    }

    /*
        findOneAndUpdate returns the document, or null if no document matched the filter
     */
    private static ResponseEntity<Void> response(MongoFindOneAndUpdateData updated) {
        return ResponseEntity.status(updated != null ? 200 : 404).build();
    }

    /**
     * findOneAndUpdate(Bson, Bson)
     */
    @PutMapping("findOneAndUpdateByCityAndAge")
    public ResponseEntity<Void> findOneAndUpdateByCityAndAge(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE).findOneAndUpdate(filter(city, minAge), update()));
    }

    /**
     * findOneAndUpdate(Bson, Bson, FindOneAndUpdateOptions)
     */
    @PutMapping("findOneAndUpdateByCityAndAgeWithOptions")
    public ResponseEntity<Void> findOneAndUpdateByCityAndAgeWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_WITH_OPTIONS).findOneAndUpdate(filter(city, minAge), update(), options()));
    }

    /**
     * findOneAndUpdate(ClientSession, Bson, Bson)
     */
    @PutMapping("findOneAndUpdateByCityAndAgeInSession")
    public ResponseEntity<Void> findOneAndUpdateByCityAndAgeInSession(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_IN_SESSION).findOneAndUpdate(session, filter(city, minAge), update()));
        }
    }

    /**
     * findOneAndUpdate(ClientSession, Bson, Bson, FindOneAndUpdateOptions)
     */
    @PutMapping("findOneAndUpdateByCityAndAgeInSessionWithOptions")
    public ResponseEntity<Void> findOneAndUpdateByCityAndAgeInSessionWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS).findOneAndUpdate(session, filter(city, minAge), update(), options()));
        }
    }

    /**
     * findOneAndUpdate(Bson, List<? extends Bson>)
     */
    @PutMapping("findOneAndUpdateByCityAndAgeWithPipeline")
    public ResponseEntity<Void> findOneAndUpdateByCityAndAgeWithPipeline(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_WITH_PIPELINE).findOneAndUpdate(filter(city, minAge), pipeline()));
    }

    /**
     * findOneAndUpdate(Bson, List<? extends Bson>, FindOneAndUpdateOptions)
     */
    @PutMapping("findOneAndUpdateByCityAndAgeWithPipelineAndOptions")
    public ResponseEntity<Void> findOneAndUpdateByCityAndAgeWithPipelineAndOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_WITH_PIPELINE_AND_OPTIONS).findOneAndUpdate(filter(city, minAge), pipeline(), options()));
    }

    /**
     * findOneAndUpdate(ClientSession, Bson, List<? extends Bson>)
     */
    @PutMapping("findOneAndUpdateByCityAndAgeInSessionWithPipeline")
    public ResponseEntity<Void> findOneAndUpdateByCityAndAgeInSessionWithPipeline(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE).findOneAndUpdate(session, filter(city, minAge), pipeline()));
        }
    }

    /**
     * findOneAndUpdate(ClientSession, Bson, List<? extends Bson>, FindOneAndUpdateOptions)
     */
    @PutMapping("findOneAndUpdateByCityAndAgeInSessionWithPipelineAndOptions")
    public ResponseEntity<Void> findOneAndUpdateByCityAndAgeInSessionWithPipelineAndOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(FIND_ONE_AND_UPDATE_BY_CITY_AND_AGE_IN_SESSION_WITH_PIPELINE_AND_OPTIONS).findOneAndUpdate(session, filter(city, minAge), pipeline(), options()));
        }
    }
}
