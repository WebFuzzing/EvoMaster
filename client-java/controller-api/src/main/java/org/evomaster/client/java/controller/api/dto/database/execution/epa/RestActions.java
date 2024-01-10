package org.evomaster.client.java.controller.api.dto.database.execution.epa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Objects;

public class RestActions {
    @JsonProperty("enabledRestActions")
    public HashSet<RestAction> enabledRestActions;

    public RestActions() {
        this.enabledRestActions = new HashSet<>();
    }

    public RestActions(@JsonProperty("enabledRestActions") HashSet<RestAction> enabledRestActions) {
        this.enabledRestActions = enabledRestActions;
    }

    public String toStringForEPA() {
        return enabledRestActions.stream().map(RestAction::toString).sorted().reduce((s, a) ->
                String.format("%s, \\n%s", s, a)).orElse("");
    }
}
