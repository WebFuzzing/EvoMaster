package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonObject;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.operations.GeoIntersectsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Collections;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/** Parses {@code $geoIntersects} queries with a GeoJSON LineString, Polygon, MultiPolygon, MultiPoint, or MultiLineString. */
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
        GeoJsonObject geometry = parseGeometry(getValue(value, "$geometry"));
        if (geometry == null) {
            return null;
        }
        return new GeoIntersectsOperation(fieldName, geometry);
    }

    private GeoJsonObject parseGeometry(Object geometry) {
        try {
            return GeoJsonUtils.toGeoJsonLineString(geometry);
        } catch (IllegalArgumentException e) {
            // not a supported GeoJSON LineString, try another geometry type
        }
        try {
            return GeoJsonUtils.toGeoJsonPolygon(geometry);
        } catch (IllegalArgumentException e) {
            // not a supported GeoJSON Polygon
        }
        try {
            return GeoJsonUtils.toGeoJsonMultiPolygon(geometry);
        } catch (IllegalArgumentException e) {
            // not a supported GeoJSON MultiPolygon
        }
        try {
            return GeoJsonUtils.toGeoJsonMultiPoint(geometry);
        } catch (IllegalArgumentException e) {
            // not a supported GeoJSON MultiPoint
        }
        try {
            return GeoJsonUtils.toGeoJsonMultiLineString(geometry);
        } catch (IllegalArgumentException e) {
            // not a supported GeoJSON MultiLineString
        }
        return null;
    }
}
