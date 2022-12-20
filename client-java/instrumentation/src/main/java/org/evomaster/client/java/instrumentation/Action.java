package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.*;

/**
 * Created by arcuri82 on 16-Sep-19.
 */
public class Action implements Serializable {

    private final int index;

    private final String name;

    /**
     * A list (possibly empty) of String values used in the action.
     * This info can be used for different kinds of taint analysis, eg
     * to check how such values are used in the SUT
     */
    private final Set<String> inputVariables;

    /**
     * A map of hostname and WireMock IP to mock external service calls.
     */
    private final Map<String, String> externalServiceMapping;

    private final Map<String, String> localDNSMapping;

    public Action(int index, String name, Collection<String> inputVariables, Map<String, String> externalServiceMapping, Map<String, String> localDNSMapping) {
        this.index = index;
        this.name = name;
        this.inputVariables = Collections.unmodifiableSet(new HashSet<>(inputVariables));
        this.externalServiceMapping = Collections.unmodifiableMap(new HashMap<>(externalServiceMapping));
        this.localDNSMapping = Collections.unmodifiableMap(new HashMap<>(localDNSMapping));
    }

    public int getIndex() {
        return index;
    }

    public Set<String> getInputVariables() {
        return inputVariables;
    }

    public Map<String, String> getExternalServiceMapping() { return externalServiceMapping; }

    public Map<String, String> getLocalDNSMapping() { return localDNSMapping; }

    public String getName() {
        return name;
    }
}
