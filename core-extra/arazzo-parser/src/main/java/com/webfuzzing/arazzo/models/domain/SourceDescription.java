package com.webfuzzing.arazzo.models.domain;

/**
 * Representing the model Source Description Object
 * Describes a source description (such as an OpenAPI description)
 * that will be referenced by one or more workflows described within an Arazzo Description
 */
public class SourceDescription {
    /**
     * A unique name for the source description.
     */
    private String name;

    /**
     * A URL to a source description to be used by a workflow.
     */
    private  String url;

    /**
     * The type of source description. Possible values are "openapi" or "arazzo".
     */
    private String type;

    public SourceDescription() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
