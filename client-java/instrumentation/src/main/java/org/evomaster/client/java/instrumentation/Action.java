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

    /**
     * A map of external service domain name against to the local IP
     * address used inside external service handling.
     */
    private final Map<String, String> localAddressMapping;

    /**
     * A list of external services, which will be skipped from handling.
     */
    private final List<ExternalService> skippedExternalServices;

    public Action(int index, String name, Collection<String> inputVariables, Map<String, String> externalServiceMapping, Map<String, String> localAddressMapping, List<ExternalService> skippedExternalServices) {
        this.index = index;
        this.name = name;
        this.inputVariables = Collections.unmodifiableSet(new HashSet<>(inputVariables));
        this.externalServiceMapping = Collections.unmodifiableMap(new HashMap<>(externalServiceMapping));
        this.localAddressMapping = Collections.unmodifiableMap(new HashMap<>(localAddressMapping));
        this.skippedExternalServices = Collections.unmodifiableList(new ArrayList<>(skippedExternalServices));
    }

    public int getIndex() {
        return index;
    }

    public Set<String> getInputVariables() {
        return inputVariables;
    }

    public Map<String, String> getExternalServiceMapping() {
        return externalServiceMapping;
    }

    public Map<String, String> getLocalAddressMapping() {
        return localAddressMapping;
    }

    public List<ExternalService> getSkippedExternalServices() {
        return skippedExternalServices;
    }

    public String getName() {
        return name;
    }
}
