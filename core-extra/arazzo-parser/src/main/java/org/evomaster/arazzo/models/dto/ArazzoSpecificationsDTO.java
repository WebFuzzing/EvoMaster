package org.evomaster.arazzo.models.dto;

import org.evomaster.arazzo.models.domain.Components;
import org.evomaster.arazzo.models.domain.InfoArazzo;
import org.evomaster.arazzo.models.domain.SourceDescription;

import java.util.List;

/**
 * Representing the ArazzoSpecifications (DTO)
 * Used for direct document parsing
 */
public class ArazzoSpecificationsDTO {
    private String arazzo;
    private InfoArazzo info;
    private List<SourceDescription> sourceDescriptions;
    private List<WorkflowDTO> workflows;
    private Components components;

    public ArazzoSpecificationsDTO() {
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

    public List<WorkflowDTO> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<WorkflowDTO> workflows) {
        this.workflows = workflows;
    }

    public Components getComponents() {
        return components;
    }

    public void setComponents(Components components) {
        this.components = components;
    }
}
