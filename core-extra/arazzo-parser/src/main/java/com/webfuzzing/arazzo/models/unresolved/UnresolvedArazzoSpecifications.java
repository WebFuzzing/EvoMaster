package com.webfuzzing.arazzo.models.unresolved;

import com.webfuzzing.arazzo.models.domain.Components;
import com.webfuzzing.arazzo.models.domain.InfoArazzo;
import com.webfuzzing.arazzo.models.domain.SourceDescription;

import java.util.List;

/**
 * Jackson-deserializable representation of the root Arazzo Specification Object
 * with unresolved references. Mutable intermediate model used during parsing;
 * mapped to the immutable domain {@link com.webfuzzing.arazzo.models.domain.ArazzoSpecifications}
 * by {@link com.webfuzzing.arazzo.mapper.ArazzoMapper}.
 */
public class UnresolvedArazzoSpecifications {
    private String arazzo;
    private InfoArazzo info;
    private List<SourceDescription> sourceDescriptions;
    private List<UnresolvedWorkflow> workflows;
    private Components components;

    public UnresolvedArazzoSpecifications() {
    }

    public String getArazzo() {
        return arazzo;
    }

    public void setArazzo(String arazzo) {
        this.arazzo = arazzo;
    }

    public InfoArazzo getInfo() {
        return info;
    }

    public void setInfo(InfoArazzo info) {
        this.info = info;
    }

    public List<SourceDescription> getSourceDescriptions() {
        return sourceDescriptions;
    }

    public void setSourceDescriptions(List<SourceDescription> sourceDescriptions) {
        this.sourceDescriptions = sourceDescriptions;
    }

    public List<UnresolvedWorkflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<UnresolvedWorkflow> workflows) {
        this.workflows = workflows;
    }

    public Components getComponents() {
        return components;
    }

    public void setComponents(Components components) {
        this.components = components;
    }
}
