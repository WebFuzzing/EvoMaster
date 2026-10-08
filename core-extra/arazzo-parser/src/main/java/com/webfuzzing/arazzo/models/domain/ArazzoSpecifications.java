package com.webfuzzing.arazzo.models.domain;

import java.util.List;

/**
 * Representing the model Arazzo Specification Object
 * This is the root object of the Arazzo Description
 * Original info from Arazzo: <a href="https://spec.openapis.org/arazzo/v1.0.1.html#arazzo-specification-object">...</a>
 */
public class ArazzoSpecifications {
    /**
     * This string MUST be the version number of the Arazzo Specification that the Arazzo Description uses
     */
    private String arazzo;

    /**
     * Provides metadata about the workflows contain within the Arazzo Description.
     */
    private InfoArazzo info;

    /**
     * A list of source descriptions.
     */
    private List<SourceDescription> sourceDescriptions;

    /**
     * A list of workflows.
     */
    private List<Workflow> workflows;

    /**
     * An element to hold various schemas for the Arazzo Description.
     */
    private Components components;

    private ArazzoSpecifications(Builder builder) {
        this.arazzo = builder.arazzo;
        this.info = builder.info;
        this.sourceDescriptions = builder.sourceDescriptions;
        this.workflows = builder.workflows;
        this.components = builder.components;
    }

    public String getArazzo() {
        return arazzo;
    }

    public InfoArazzo getInfo() {
        return info;
    }

    public List<SourceDescription> getSourceDescriptions() {
        return sourceDescriptions;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public Components getComponents() {
        return components;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String arazzo;
        private InfoArazzo info;
        private List<SourceDescription> sourceDescriptions;
        private List<Workflow> workflows;
        private Components components;

        public Builder arazzo(String arazzo) { this.arazzo = arazzo; return this; }
        public Builder info(InfoArazzo info) { this.info = info; return this; }
        public Builder sourceDescriptions(List<SourceDescription> sourceDescriptions) { this.sourceDescriptions = sourceDescriptions; return this; }
        public Builder workflows(List<Workflow> workflows) { this.workflows = workflows; return this; }
        public Builder components(Components components) { this.components = components; return this; }

        public ArazzoSpecifications build() {
            return new ArazzoSpecifications(this);
        }

    }
}
