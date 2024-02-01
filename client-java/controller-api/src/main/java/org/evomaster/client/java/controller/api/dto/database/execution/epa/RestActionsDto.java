package org.evomaster.client.java.controller.api.dto.database.execution.epa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;

public class RestActionsDto {
    @JsonProperty("enabledRestActions")
    public HashSet<RestActionDto> enabledRestActions;

    public RestActionsDto() {
        this.enabledRestActions = new HashSet<>();
    }

    public RestActionsDto(@JsonProperty("enabledRestActions") HashSet<RestActionDto> enabledRestActions) {
        this.enabledRestActions = enabledRestActions;
    }
}
