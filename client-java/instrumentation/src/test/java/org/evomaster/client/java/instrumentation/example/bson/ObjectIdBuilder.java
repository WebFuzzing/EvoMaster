package org.evomaster.client.java.instrumentation.example.bson;

import org.bson.types.ObjectId;

public interface ObjectIdBuilder {

    ObjectId buildNewObjectId(String hexString);
}
