package org.evomaster.client.java.instrumentation.shared;

import java.io.Serializable;

public enum TaintType implements Serializable {

    NONE,

    FULL_MATCH,

    PARTIAL_MATCH;


    public boolean isTainted(){
        return this != NONE;
    }

    public boolean isFullMatch(){
        return this == FULL_MATCH;
    }

    public boolean isPartialMatch(){
        return this == PARTIAL_MATCH;
    }
}
