package com.mongo.findoneanddelete;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.Sorts;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deletes documents directly through the Mongo driver (MongoCollection.findOneAndDelete),
 * not through Spring Data, so the filter is only visible at the driver level.
 * There is one endpoint for each overload of findOneAndDelete.
 */
@RestController
@RequestMapping(path = "/mongofindoneanddelete")
public class MongoFindOneAndDeleteController {

    /*
        Each endpoint uses its own collection, so that a document that
        makes one endpoint return 200 cannot accidentally do it for another one.
     */
    private static final String FIND_ONE_AND_DELETE_BY_CITY_AND_AGE = "findOneAndDeleteByCityAndAgeCollection";
    private static final String FIND_ONE_AND_DELETE_BY_CITY_AND_AGE_WITH_OPTIONS = "findOneAndDeleteByCityAndAgeWithOptionsCollection";
    private static final String FIND_ONE_AND_DELETE_BY_CITY_AND_AGE_IN_SESSION = "findOneAndDeleteByCityAndAgeInSessionCollection";
    private static final String FIND_ONE_AND_DELETE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS = "findOneAndDeleteByCityAndAgeInSessionWithOptionsCollection";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    /*
        findOneAndDelete returns the deleted document, so (unlike count, delete or update)
        the driver needs a codec to decode it into our own class
     */
    private static final CodecRegistry POJO_CODEC_REGISTRY = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    private MongoCollection<MongoFindOneAndDeleteData> collection(String collectionName) {
        return mongoTemplate.getDb()
                .getCollection(collectionName, MongoFindOneAndDeleteData.class)
                .withCodecRegistry(POJO_CODEC_REGISTRY);
    }

    private static Bson filter(String city, int minAge) {
        return Filters.and(Filters.eq("city", city), Filters.gte("age", minAge));
    }

    private static FindOneAndDeleteOptions options() {
        return new FindOneAndDeleteOptions().sort(Sorts.ascending("age"));
    }

    /*
        findOneAndDelete returns the deleted document, or null if no document matched the filter
     */
    private static ResponseEntity<Void> response(MongoFindOneAndDeleteData deleted) {
        return ResponseEntity.status(deleted != null ? 200 : 404).build();
    }

    /**
     * findOneAndDelete(Bson)
     */
    @DeleteMapping("findOneAndDeleteByCityAndAge")
    public ResponseEntity<Void> findOneAndDeleteByCityAndAge(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(FIND_ONE_AND_DELETE_BY_CITY_AND_AGE).findOneAndDelete(filter(city, minAge)));
    }

    /**
     * findOneAndDelete(Bson, FindOneAndDeleteOptions)
     */
    @DeleteMapping("findOneAndDeleteByCityAndAgeWithOptions")
    public ResponseEntity<Void> findOneAndDeleteByCityAndAgeWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(FIND_ONE_AND_DELETE_BY_CITY_AND_AGE_WITH_OPTIONS).findOneAndDelete(filter(city, minAge), options()));
    }

    /**
     * findOneAndDelete(ClientSession, Bson)
     */
    @DeleteMapping("findOneAndDeleteByCityAndAgeInSession")
    public ResponseEntity<Void> findOneAndDeleteByCityAndAgeInSession(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(FIND_ONE_AND_DELETE_BY_CITY_AND_AGE_IN_SESSION).findOneAndDelete(session, filter(city, minAge)));
        }
    }

    /**
     * findOneAndDelete(ClientSession, Bson, FindOneAndDeleteOptions)
     */
    @DeleteMapping("findOneAndDeleteByCityAndAgeInSessionWithOptions")
    public ResponseEntity<Void> findOneAndDeleteByCityAndAgeInSessionWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(FIND_ONE_AND_DELETE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS).findOneAndDelete(session, filter(city, minAge), options()));
        }
    }
}
