package org.evomaster.arazzo.models.domain;

/**
 * Representing the model Info Object
 * This is the root object of the Arazzo Description
 */
public class InfoArazzo {
    private String title;
    private String summary;
    private String description;
    private String version;

    public InfoArazzo() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
