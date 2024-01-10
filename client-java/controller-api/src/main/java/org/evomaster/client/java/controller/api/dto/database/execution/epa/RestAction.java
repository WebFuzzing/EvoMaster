package org.evomaster.client.java.controller.api.dto.database.execution.epa;

import com.fasterxml.jackson.annotation.JsonProperty;


public class RestAction {
    @JsonProperty("verb")
    public String verb;

    @JsonProperty("path")
    public String path;

    public RestAction(@JsonProperty("verb") String verb, @JsonProperty("path")String path) {
        this.verb = verb;
        this.path = path;
    }

    @Override
    public String toString() {
        return String.format("%s::%s", verb, path);
    }
}
