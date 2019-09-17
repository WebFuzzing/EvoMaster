package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by arcuri82 on 16-Sep-19.
 */
public class Action implements Serializable {

    private final int index;

    /**
     * A list (possibly empty) of String values used in the action.
     * This info can be used for different kinds of taint analysis, eg
     * to check how such values are used in the SUT
     */
    private Set<String> inputVariables;

    public Action(int index, Collection<String> inputVariables) {
        this.index = index;
        this.inputVariables = Collections.unmodifiableSet(new HashSet<>(inputVariables));
    }

    public int getIndex() {
        return index;
    }

    public Set<String> getInputVariables() {
        return inputVariables;
    }
}
