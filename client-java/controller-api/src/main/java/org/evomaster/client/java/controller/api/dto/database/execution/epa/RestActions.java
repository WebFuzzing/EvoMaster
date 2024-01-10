package org.evomaster.client.java.controller.api.dto.database.execution.epa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;

public class RestActions {
    @JsonProperty("enabledRestActions")
    public HashSet<RestAction> enabledRestActions;

    public RestActions() {
        this.enabledRestActions = new HashSet<>();
    }

    public RestActions(@JsonProperty("enabledRestActions") HashSet<RestAction> enabledRestActions) {
        this.enabledRestActions = enabledRestActions;
    }
}
