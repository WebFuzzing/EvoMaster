package org.evomaster.arazzo.models.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class RequestBody {
    private String contentType;
    private JsonNode payload;
    private List<PayloadReplacement> replacements;

    public RequestBody() {
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public List<PayloadReplacement> getReplacements() {
        return replacements;
    }

    public void setReplacements(List<PayloadReplacement> replacements) {
        this.replacements = replacements;
    }
}
