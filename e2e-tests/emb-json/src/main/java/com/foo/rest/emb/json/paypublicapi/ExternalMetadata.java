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
    public static final int MAX_KEY_VALUE_PAIRS = 10;
    public static final int MIN_KEY_LENGTH = 1;
    public static final int MAX_KEY_LENGTH = 30;
    public static final int MAX_VALUE_LENGTH = 100;
//    @ValidExternalMetadata
    private final Map<String, Object> metadata;

    public ExternalMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap(metadata);
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(this.metadata);
    }
}
