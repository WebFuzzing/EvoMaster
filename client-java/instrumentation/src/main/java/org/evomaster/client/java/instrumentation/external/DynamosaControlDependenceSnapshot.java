package org.evomaster.client.java.instrumentation.external;

import org.evomaster.client.java.instrumentation.shared.dto.ControlDependenceGraphDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Serializable payload used between the Java Agent and the controller to
 * exchange newly discovered control-dependence graphs together with the
 * next index to request.
 */
public class DynamosaControlDependenceSnapshot implements Serializable {

    private static final long serialVersionUID = 6779255129756570792L;

    private final List<ControlDependenceGraphDto> graphs;
    private final int nextIndex;

    public DynamosaControlDependenceSnapshot(List<ControlDependenceGraphDto> graphs, int nextIndex) {
        this.graphs = graphs == null ? new ArrayList<>() : new ArrayList<>(graphs);
        this.nextIndex = Math.max(nextIndex, 0);
    }

    public List<ControlDependenceGraphDto> getGraphs() {
        return Collections.unmodifiableList(graphs);
    }

    public int getNextIndex() {
        return nextIndex;
    }
}


