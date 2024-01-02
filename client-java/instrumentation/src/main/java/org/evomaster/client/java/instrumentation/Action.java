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
     * This info can be used for different kinds of taint analysis, e.g.
     * to check how such values are used in the SUT
     */
    private final Set<String> inputVariables;

    /**
     * Map of external services mapping. Key is the WireMock signature
     * (use protocol, remote hostname, and port), and value contains information
     * about remote hostname, mock server local IP address, state
     * (active or inactive) and WireMock signature.
     */
    private final Map<String, ExternalServiceMapping> externalServiceMapping;

    /**
     * A map of external service domain name against to the local IP
     * address used inside external service handling.
     */
    private final Map<String, String> localAddressMapping;

    /**
     * A list of external services, which will be skipped from handling.
     */
    private final List<ExternalService> skippedExternalServices;

    public Action(int index, String name, Collection<String> inputVariables, Map<String, ExternalServiceMapping> externalServiceMapping, Map<String, String> localAddressMapping, List<ExternalService> skippedExternalServices) {
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

    public Map<String, ExternalServiceMapping> getExternalServiceMapping() {
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
