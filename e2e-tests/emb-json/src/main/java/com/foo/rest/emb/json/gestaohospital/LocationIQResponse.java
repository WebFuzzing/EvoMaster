package com.foo.rest.emb.json.gestaohospital;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class LocationIQResponse {

    private String placeId;
    private String licence;
    private String osmType;
    private String osmId;
    private List<String> boundingbox;
    private String lat;
    private String lon;
    private String displayName;
    private String classfication;
    private String type;
    private String importance;
    private String icon;


    public LocationIQResponse() {
        boundingbox  = new ArrayList<>();
    }

    @JsonCreator
    public LocationIQResponse(
            @JsonProperty("place_id") String placeId,
            @JsonProperty("licence") String licence,
            @JsonProperty("osm_type") String osmType,
            @JsonProperty("osm_id") String osmId,
            @JsonProperty("boundingbox") List<String> boundingbox,
            @JsonProperty("lat") String lat,
            @JsonProperty("lon") String lon,
            @JsonProperty("display_name") String displayName,
            @JsonProperty("class") String classfication,
            @JsonProperty("type") String type,
            @JsonProperty("importance") String importance,
            @JsonProperty("icon") String icon)
    {
        this.placeId = placeId;
        this.licence = licence;
        this.osmType = osmType;
        this.osmId = osmId;
        this.boundingbox = boundingbox;
        this.lat = lat;
        this.lon = lon;
        this.displayName = displayName;
        this.classfication = classfication;
        this.type = type;
        this.importance = importance;
        this.icon = icon;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getLicence() {
        return licence;
    }

    public String getOsmType() {
        return osmType;
    }

    public String getOsmId() {
        return osmId;
    }

    public List<String> getBoundingbox() {
        return boundingbox;
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getClassfication() {
        return classfication;
    }

    public String getType() {
        return type;
    }

    public String getImportance() {
        return importance;
    }

    public String getIcon() {
        return icon;
    }

}

