package com.foo.rest.emb.json.paypublicapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This code is taken from pay-java-commons
 * G: https://github.com/alphagov/pay-java-commons
 * L: MIT
 * P: model/src/main/java/uk/gov/service/payments/commons/model/charge/ExternalMetadata.java
 */
public class ExternalMetadata {

    private final Map<String, Object> metadata;

    public ExternalMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap(metadata);
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(this.metadata);
    }
}
