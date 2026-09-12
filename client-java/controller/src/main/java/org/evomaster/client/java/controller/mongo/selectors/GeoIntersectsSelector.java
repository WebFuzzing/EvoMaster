package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.operations.GeoIntersectsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Collections;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/** Parses {@code $geoIntersects} queries with a GeoJSON LineString. */
public class GeoIntersectsSelector extends SingleConditionQuerySelector {

    @Override
    protected String operator() {
        return "$geoIntersects";
    }

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value == null || !isBsonDocument(value)
                || !documentKeys(value).equals(Collections.singleton("$geometry"))) {
            return null;
        }
        try {
            return new GeoIntersectsOperation(fieldName,
                    GeoJsonUtils.toGeoJsonLineString(getValue(value, "$geometry")));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
