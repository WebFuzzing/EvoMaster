package org.evomaster.client.java.controller.api.dto.database.execution.epa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;

public class EnabledRestActionsDto {
    @JsonProperty("enabledRestActions")
    public HashSet<RestActionDto> enabledRestActions;

    public EnabledRestActionsDto() {
        this.enabledRestActions = new HashSet<>();
    }

    public EnabledRestActionsDto(@JsonProperty("enabledRestActions") HashSet<RestActionDto> enabledRestActions) {
        this.enabledRestActions = enabledRestActions;
    }
}
