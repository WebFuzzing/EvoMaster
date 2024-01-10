package org.evomaster.client.java.controller.api.dto.database.execution.epa;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Enabled {

    @JsonProperty("associatedRestAction")
    public RestAction associatedRestAction;

    @JsonProperty("enabledRestActions")
    public RestActions enabledRestActions;

    public Enabled() {
        this.associatedRestAction = null;
        this.enabledRestActions = new RestActions();
    }

    public Enabled(@JsonProperty("associatedRestAction") RestAction lastRestAction,
                   @JsonProperty("enabledRestActions") RestActions enabledRestActions) {
        this.associatedRestAction = lastRestAction;
        this.enabledRestActions = enabledRestActions;
    }
}
