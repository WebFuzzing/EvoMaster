package org.evomaster.client.java.controller.mongo.geometry;

import java.util.Collections;
import java.util.List;

public class GeoJsonMultiLineString extends GeoJsonGeometry {

    public static final String MULTI_LINE_STRING_TYPE = "MultiLineString";

    private final List<GeoJsonLineString> lineStrings;

    public GeoJsonMultiLineString(List<GeoJsonLineString> lineStrings) {
        super(MULTI_LINE_STRING_TYPE);
        this.lineStrings = Collections.unmodifiableList(lineStrings);
    }

    public List<GeoJsonLineString> getLineStrings() {
        return lineStrings;
    }

    @Override
    public boolean hasArea() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GeoJsonMultiLineString{");
        sb.append("lineStrings=[");
        for (int i = 0; i < lineStrings.size(); i++) {
            sb.append(lineStrings.get(i));
            if (i < lineStrings.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeoJsonMultiLineString)) {
            return false;
        }
        GeoJsonMultiLineString that = (GeoJsonMultiLineString) obj;
        return lineStrings.equals(that.lineStrings);
    }

    @Override
    public int hashCode() {
        return lineStrings.hashCode();
    }
}
