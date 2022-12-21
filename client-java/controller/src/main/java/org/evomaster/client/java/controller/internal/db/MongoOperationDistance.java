package org.evomaster.client.java.controller.internal.db;

import org.bson.conversions.Bson;

public class MongoOperationDistance {

    public final Bson bson;

    public final double distance;


    public MongoOperationDistance(Bson bson, double distance) {
        this.bson = bson;
        this.distance = distance;
    }
}
