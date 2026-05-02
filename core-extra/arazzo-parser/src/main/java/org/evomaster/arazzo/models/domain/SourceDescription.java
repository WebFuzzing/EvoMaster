package org.evomaster.arazzo.models.domain;

/**
 * Representing the model Source Description Object
 * Describes a source description (such as an OpenAPI description)
 * that will be referenced by one or more workflows described within an Arazzo Description
 */
public class SourceDescription {
    private String name;
    private  String url;
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
