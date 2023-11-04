package org.evomaster.client.java.instrumentation.object;

/**
 * Point: {type: "Point", coordinates: [[longitude,latitude]]}
 * type "Point" is case-sensitive
 * <a href="https://datatracker.ietf.org/doc/html/rfc7946#section-3.1.2">GeoJsonPoint Documentation</a>
 */
public class GeoJsonPointToOasConverter extends CustomTypeToOasConverter {
    @Override
    public String convert() {
        return "{\"type\":\"object\", \"properties\": {\"coordinates\": {\"type\":\"array\", \"items\":{\"type\":\"number\", \"format\":\"double\"}, \"minItems\": 2,  \"maxItems\": 2}, \"type\": {\"type\":\"string\", \"enum\":[\"Point\"]}}, \"required\": [\"coordinates\", \"type\"]}";
    }

    @Override
    public boolean isInstanceOf(Class<?> klass) {
        return klass.getName().equals("org.springframework.data.mongodb.core.geo.GeoJsonPoint");
    }
}
