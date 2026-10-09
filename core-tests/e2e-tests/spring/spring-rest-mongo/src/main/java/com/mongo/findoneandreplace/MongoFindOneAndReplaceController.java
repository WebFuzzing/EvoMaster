package com.mongo.findoneandreplace;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
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

/**
 * Replaces documents directly through the Mongo driver (MongoCollection.findOneAndReplace),
 * not through Spring Data, so the filter is only visible at the driver level.
 * There is one endpoint for each overload of findOneAndReplace.
 */
@RestController
@RequestMapping(path = "/mongofindoneandreplace")
public class MongoFindOneAndReplaceController {

    /*
        Each endpoint uses its own collection, so that a document that
        makes one endpoint return 200 cannot accidentally do it for another one.
     */
    private static final String FIND_ONE_AND_REPLACE_BY_CITY_AND_AGE = "findOneAndReplaceByCityAndAgeCollection";
    private static final String FIND_ONE_AND_REPLACE_BY_CITY_AND_AGE_WITH_OPTIONS = "findOneAndReplaceByCityAndAgeWithOptionsCollection";
    private static final String FIND_ONE_AND_REPLACE_BY_CITY_AND_AGE_IN_SESSION = "findOneAndReplaceByCityAndAgeInSessionCollection";
    private static final String FIND_ONE_AND_REPLACE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS = "findOneAndReplaceByCityAndAgeInSessionWithOptionsCollection";

    /*
        findOneAndReplace returns the document, so (unlike count, delete or update)
        the driver needs a codec to decode it into our own class
     */
    private static final CodecRegistry POJO_CODEC_REGISTRY = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    private MongoCollection<MongoFindOneAndReplaceData> collection(String collectionName) {
        return mongoTemplate.getDb()
                .getCollection(collectionName, MongoFindOneAndReplaceData.class)
                .withCodecRegistry(POJO_CODEC_REGISTRY);
    }

    private static Bson filter(String city, int minAge) {
        return Filters.and(Filters.eq("city", city), Filters.gte("age", minAge));
    }

    /*
        the replacement has no id, so the replaced document keeps its own _id
     */
    private static MongoFindOneAndReplaceData replacement(String city, int minAge) {
        return new MongoFindOneAndReplaceData(city, minAge, true);
    }

    private static FindOneAndReplaceOptions options() {
        return new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);
    }

    /*
        findOneAndReplace returns the document, or null if no document matched the filter
     */
    private static ResponseEntity<Void> response(MongoFindOneAndReplaceData replaced) {
        return ResponseEntity.status(replaced != null ? 200 : 404).build();
    }

    /**
     * findOneAndReplace(Bson, TDocument)
     */
    @PutMapping("findOneAndReplaceByCityAndAge")
    public ResponseEntity<Void> findOneAndReplaceByCityAndAge(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(FIND_ONE_AND_REPLACE_BY_CITY_AND_AGE).findOneAndReplace(filter(city, minAge), replacement(city, minAge)));
    }

    /**
     * findOneAndReplace(Bson, TDocument, FindOneAndReplaceOptions)
     */
    @PutMapping("findOneAndReplaceByCityAndAgeWithOptions")
    public ResponseEntity<Void> findOneAndReplaceByCityAndAgeWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(collection(FIND_ONE_AND_REPLACE_BY_CITY_AND_AGE_WITH_OPTIONS).findOneAndReplace(filter(city, minAge), replacement(city, minAge), options()));
    }

    /**
     * findOneAndReplace(ClientSession, Bson, TDocument)
     */
    @PutMapping("findOneAndReplaceByCityAndAgeInSession")
    public ResponseEntity<Void> findOneAndReplaceByCityAndAgeInSession(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(FIND_ONE_AND_REPLACE_BY_CITY_AND_AGE_IN_SESSION).findOneAndReplace(session, filter(city, minAge), replacement(city, minAge)));
        }
    }

    /**
     * findOneAndReplace(ClientSession, Bson, TDocument, FindOneAndReplaceOptions)
     */
    @PutMapping("findOneAndReplaceByCityAndAgeInSessionWithOptions")
    public ResponseEntity<Void> findOneAndReplaceByCityAndAgeInSessionWithOptions(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        try (ClientSession session = mongoClient.startSession()) {
            return response(collection(FIND_ONE_AND_REPLACE_BY_CITY_AND_AGE_IN_SESSION_WITH_OPTIONS).findOneAndReplace(session, filter(city, minAge), replacement(city, minAge), options()));
        }
    }
}
