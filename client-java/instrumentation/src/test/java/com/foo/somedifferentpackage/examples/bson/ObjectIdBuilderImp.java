package com.foo.somedifferentpackage.examples.bson;

import org.bson.types.ObjectId;
import org.evomaster.client.java.instrumentation.example.bson.ObjectIdBuilder;

public class ObjectIdBuilderImp implements ObjectIdBuilder {

    @Override
    public ObjectId buildNewObjectId(String hexString) {
        return new ObjectId(hexString);
    }


}