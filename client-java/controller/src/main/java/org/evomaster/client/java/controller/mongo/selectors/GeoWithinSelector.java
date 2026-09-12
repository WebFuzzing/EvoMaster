package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.operations.GeoWithinOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Collections;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/** Parses {@code $geoWithin} queries with a GeoJSON {@code $geometry}. */
public class GeoWithinSelector extends SingleConditionQuerySelector {

    @Override
    protected String operator() {
        return "$geoWithin";
    }

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value == null || !isBsonDocument(value)
                || !documentKeys(value).equals(Collections.singleton("$geometry"))) {
            return null;
        }
        try {
            return new GeoWithinOperation(fieldName,
                    GeoJsonUtils.toGeoJsonGeometry(getValue(value, "$geometry")));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
