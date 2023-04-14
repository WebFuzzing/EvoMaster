package org.evomaster.client.java.controller.internal.db;

public class MongoOperationDistance {

    public final Object bson;

    public final double distance;


    public MongoOperationDistance(Object bson, double distance) {
        this.bson= bson;
        this.distance = distance;
    }
}
