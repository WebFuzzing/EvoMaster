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

    private static MongoLogger instance = null;

    private PrintStream outputStream = null;

    public synchronized static MongoLogger getInstance() {
        if (instance == null) {
            instance = new MongoLogger();
        }
        return instance;
    }

    public void setOutputStream(PrintStream os) {
        this.outputStream = os;
    }

    private MongoLogger() {

    }


    public void reset() {
        this.outputStream = null;
    }

    public static final String PREFIX = "MONGO_LOGGER";

    public void logFind(MongoCollection mongoCollection, Bson bson, boolean hasOperationFoundAnyDocument) {
        String dbName = mongoCollection.getNamespace().getDatabaseName();
        String collectionName = mongoCollection.getNamespace().getCollectionName();
        BsonDocument query = bson.toBsonDocument(BsonDocument.class, mongoCollection.getCodecRegistry());
        Document queryDoc = new DocumentCodec().decode(query.asBsonReader(), DecoderContext.builder().build());

        LoggedExecutedFindOperation log = new LoggedExecutedFindOperation(dbName, collectionName, queryDoc, hasOperationFoundAnyDocument);
        String jsonString = new Gson().toJson(log);
        String mongoOperation = String.format("%s:%s", PREFIX, jsonString);

        if (outputStream == null) {
            // System.out could be changed during execution
            System.out.println(mongoOperation);
        } else {
            outputStream.println(mongoOperation);
        }
    }
}
