package org.evomaster.client.java.instrumentation.mongo;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;

import java.io.PrintStream;

public class MongoLogger {

    private static final PrintStream DEFAULT_OUT = System.out;

    private static MongoLogger instance;

    private PrintStream outputStream;

    public synchronized static MongoLogger getInstance() {
        if (instance == null) {
            instance = new MongoLogger();
        }
        return instance;
    }

    public void setOutputStream(PrintStream os) {
        outputStream = os;
    }

    private MongoLogger() {
        initLogger();
    }

    private void initLogger() {
        outputStream = DEFAULT_OUT;
    }

    public void reset() {
        initLogger();
    }

    public static final String PREFIX = "MONGO_LOGGER";

    public void logFind(MongoCollection mongoCollection, Bson bson) {
        String dbName = mongoCollection.getNamespace().getDatabaseName();
        String collectionName = mongoCollection.getNamespace().getCollectionName();
        BsonDocument query = bson.toBsonDocument(BsonDocument.class, mongoCollection.getCodecRegistry());
        Document queryDoc = new DocumentCodec().decode(query.asBsonReader(), DecoderContext.builder().build());

        MongoFindOperation op = new MongoFindOperation(dbName, collectionName, queryDoc);
        String jsonString = new Gson().toJson(op);
        String mongoOperation = String.format("%s:%s", PREFIX, jsonString);
        outputStream.println(mongoOperation);
    }
}
