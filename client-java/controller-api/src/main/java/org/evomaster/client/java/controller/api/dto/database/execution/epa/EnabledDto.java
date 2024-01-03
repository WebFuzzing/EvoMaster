package org.evomaster.client.java.controller.api.dto.database.execution.epa;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnabledDto {

    @JsonProperty("associatedRestAction")
    public RestActionDto associatedRestAction;

    @JsonProperty("enabledRestActions")
    public EnabledRestActionsDto enabledRestActions;

    public EnabledDto() {
        this.associatedRestAction = null;
        this.enabledRestActions = new EnabledRestActionsDto();
    }

    public EnabledDto(@JsonProperty("associatedRestAction") RestActionDto lastRestAction,
                      @JsonProperty("enabledRestActions") EnabledRestActionsDto enabledRestActions) {
        this.associatedRestAction = lastRestAction;
        this.enabledRestActions = enabledRestActions;
    }
}
