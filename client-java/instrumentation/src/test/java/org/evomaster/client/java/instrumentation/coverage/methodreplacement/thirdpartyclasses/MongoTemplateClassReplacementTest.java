package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.Document;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.MongoCollectionSchema;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.CustomTypeToOasConverter;
import org.evomaster.client.java.instrumentation.object.GeoJsonPointToOasConverter;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.GenericContainer;

import java.util.*;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

public class MongoTemplateClassReplacementTest {

    private static MongoClient mongoClient;
    private static final int MONGODB_PORT = 27017;
    private static final GenericContainer<?> mongodb = new GenericContainer<>("mongo:6.0")
            .withExposedPorts(MONGODB_PORT);

    @BeforeAll
    public static void initMongoClient() {
        mongodb.start();
        int port = mongodb.getMappedPort(MONGODB_PORT);

        MongoClientSettings.Builder builder = MongoClientSettings.builder();
        builder.applyConnectionString(new ConnectionString("mongodb://localhost:" + port + "/" + "aDatabase"));
        MongoClientSettings settings = builder
                .build();

        mongoClient = MongoClients.create(settings);

        ExecutionTracer.reset();
    }

    @AfterAll
    public static void resetExecutionTracer() {
        ExecutionTracer.reset();
    }

    private final static String DATABASE_NAME = "myDatabase";
    private final static String COLLECTION_NAME = "myCollection";

    @BeforeEach
    public void clearDatabase() {

        final MongoCollection<Document> collection = getMongoCollection();

        // delete all documents in collection (if any)
        collection.deleteMany(new Document());

        ExecutionTracer.reset();
    }

    private static MongoTemplate getMongoTemplate() {
        return new MongoTemplate(mongoClient, DATABASE_NAME);
    }

    private static MongoCollection<Document> getMongoCollection() {
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        return collection;
    }


    @Test
    public void testSave() {
        final MongoTemplate mongoTemplate = getMongoTemplate();
        MongoCollectionTestDto dto = new MongoCollectionTestDto();
        dto.name = "Jane Austin";
        dto.age = 65;
        dto.city = "Charleston";
        final String collectionName = getMongoCollection().getNamespace().getCollectionName();

        ExecutionTracer.setExecutingInitMongo(false);

        MongoTemplateClassReplacement.save(mongoTemplate, dto, collectionName);

        Query ageQuery = new Query();
        ageQuery.addCriteria(Criteria.where("age").is(65));
        List<MongoCollectionTestDto> results = mongoTemplate.find(ageQuery, MongoCollectionTestDto.class, collectionName);
        assertEquals(1, results.size());

        MongoCollectionTestDto retrievedInstance = results.get(0);
        assertEquals("Jane Austin", retrievedInstance.name);
        assertEquals(65, retrievedInstance.age);
        assertEquals("Charleston", retrievedInstance.city);

        Query cityQuery = new Query();
        cityQuery.addCriteria(Criteria.where("city").is("Rome"));
        List<MongoCollectionTestDto> noResults = mongoTemplate.find(cityQuery, MongoCollectionTestDto.class, collectionName);
        assertEquals(0, noResults.size());

    }


    @Test
    public void testFindOneByQueryEntityClassAndCollectionName() {
        final MongoTemplate mongoTemplate = getMongoTemplate();
        final String collectionName = getMongoCollection().getNamespace().getCollectionName();

        MongoCollectionTestDto dto = new MongoCollectionTestDto();
        dto.name = "Jack Austin";
        dto.age = 32;
        dto.city = "Seattle";

        mongoTemplate.save(dto, collectionName);

        ExecutionTracer.setExecutingInitMongo(false);


        Query ageQuery = new Query();
        ageQuery.addCriteria(Criteria.where("age").is(32));

        MongoCollectionTestDto retrievedInstance = MongoTemplateClassReplacement.findOne(mongoTemplate, ageQuery, MongoCollectionTestDto.class, collectionName);

        assertEquals("Jack Austin", retrievedInstance.name);
        assertEquals(32, retrievedInstance.age);
        assertEquals("Seattle", retrievedInstance.city);

        Query cityQuery = new Query();
        cityQuery.addCriteria(Criteria.where("city").is("Rome"));
        MongoCollectionTestDto noInstance = MongoTemplateClassReplacement.findOne(mongoTemplate, cityQuery, MongoCollectionTestDto.class, collectionName);

        assertNull(noInstance);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<MongoCollectionSchema> mongoCollectionToSchemaSet = additionalInfoList.get(0).getMongoCollectionTypeData();
        assertEquals(1, mongoCollectionToSchemaSet.size());

        MongoCollectionSchema mongoCollectionToSchema = mongoCollectionToSchemaSet.iterator().next();
        assertEquals(collectionName, mongoCollectionToSchema.getCollectionName());

        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String mongoCollectionTestDtoSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(MongoCollectionTestDto.class, true, converters);

        assertEquals(mongoCollectionTestDtoSchema, mongoCollectionToSchema.getCollectionSchema());


    }


    @Test
    public void testFindByQueryEntityClassAndCollectionName() {
        final MongoTemplate mongoTemplate = getMongoTemplate();
        final String collectionName = getMongoCollection().getNamespace().getCollectionName();

        MongoCollectionTestDto dto = new MongoCollectionTestDto();
        dto.name = "Jack Austin";
        dto.age = 32;
        dto.city = "Seattle";

        mongoTemplate.save(dto, collectionName);

        ExecutionTracer.setExecutingInitMongo(false);


        Query ageQuery = new Query();
        ageQuery.addCriteria(Criteria.where("age").is(32));

        List<MongoCollectionTestDto> retrievedInstances = MongoTemplateClassReplacement.find(mongoTemplate, ageQuery, MongoCollectionTestDto.class, collectionName);

        assertEquals(1, retrievedInstances.size());

        MongoCollectionTestDto retrievedInstance = retrievedInstances.get(0);
        assertEquals("Jack Austin", retrievedInstance.name);
        assertEquals(32, retrievedInstance.age);
        assertEquals("Seattle", retrievedInstance.city);

        Query cityQuery = new Query();
        cityQuery.addCriteria(Criteria.where("city").is("Rome"));
        List<MongoCollectionTestDto> noInstances = MongoTemplateClassReplacement.find(mongoTemplate, cityQuery, MongoCollectionTestDto.class, collectionName);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<MongoCollectionSchema> mongoCollectionToSchemaSet = additionalInfoList.get(0).getMongoCollectionTypeData();
        assertEquals(1, mongoCollectionToSchemaSet.size());

        MongoCollectionSchema mongoCollectionToSchema = mongoCollectionToSchemaSet.iterator().next();
        assertEquals(collectionName, mongoCollectionToSchema.getCollectionName());

        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String mongoCollectionTestDtoSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(MongoCollectionTestDto.class, true, converters);

        assertEquals(mongoCollectionTestDtoSchema, mongoCollectionToSchema.getCollectionSchema());

    }

    @Test
    public void testFindWithDifferentEntityClasses() {
        final MongoTemplate mongoTemplate = getMongoTemplate();
        final String collectionName = getMongoCollection().getNamespace().getCollectionName();
        ExecutionTracer.setExecutingInitMongo(false);

        Query cityQuery = new Query();
        cityQuery.addCriteria(Criteria.where("city").is("Rome"));

        List<MongoCollectionTestDto> noMongoCollectionTestDtos = MongoTemplateClassReplacement.find(mongoTemplate, cityQuery, MongoCollectionTestDto.class, collectionName);
        List<JacksonTestDto> noJacksonTestDtos = MongoTemplateClassReplacement.find(mongoTemplate, cityQuery, JacksonTestDto.class, collectionName);

        assertTrue(noMongoCollectionTestDtos.isEmpty());
        assertTrue(noJacksonTestDtos.isEmpty());

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<MongoCollectionSchema> mongoCollectionToSchemaSet = additionalInfoList.get(0).getMongoCollectionTypeData();
        assertEquals(2, mongoCollectionToSchemaSet.size());

        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String mongoCollectionTestDtoSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(MongoCollectionTestDto.class, true, converters);
        String jacksonTestDtoSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(JacksonTestDto.class, true, converters);

        MongoCollectionSchema mongoCollectionTestDtoToSchema = new MongoCollectionSchema(collectionName, mongoCollectionTestDtoSchema);
        MongoCollectionSchema jacksonTestDtoToSchema = new MongoCollectionSchema(collectionName, jacksonTestDtoSchema);

        assertEquals(new LinkedHashSet<>(Arrays.asList(mongoCollectionTestDtoToSchema, jacksonTestDtoToSchema)), mongoCollectionToSchemaSet);

    }
}
