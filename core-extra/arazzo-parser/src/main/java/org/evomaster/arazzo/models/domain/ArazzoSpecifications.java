package org.evomaster.arazzo.models.domain;

import java.util.List;

/**
 * Representing the model Arazzo Specification Object
 * This is the root object of the Arazzo Description
 */
public class ArazzoSpecifications {
    private String arazzo;
    private InfoArazzo info;
    private List<SourceDescription> sourceDescriptions;
    private List<Workflow> workflows;
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
