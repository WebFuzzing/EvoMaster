package org.evomaster.client.java.controller.api.dto.database.execution.epa;

import io.swagger.models.HttpMethod;


public class RestActionDto {
    public HttpMethod verb;
    public String path;

    public RestActionDto(HttpMethod verb, String path) {
        this.verb = verb;
        this.path = path;
    }
}
