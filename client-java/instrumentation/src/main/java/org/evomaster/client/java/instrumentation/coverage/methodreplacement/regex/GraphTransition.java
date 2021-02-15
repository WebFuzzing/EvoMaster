package org.evomaster.client.java.instrumentation.coverage.methodreplacement.regex;

import dk.brics.automaton.State;

public class GraphTransition {
    public enum TransitionType {
        INSERTION, DELETION, REPLACEMENT,
        /**
         * A phantom transition is an artificial transition from the sink/final states to a single artificial sink/state.
         * This is used to simplify the recursion calculation of the subpath costs.
         */
        PHANTOM
    }

    public final double cost;
    public final int fromRow;
    public final State fromState;
    public final TransitionType type;

    public GraphTransition(double cost, int fromRow, State fromState, TransitionType type) {
        this.cost = cost;
        this.fromRow = fromRow;
        this.fromState = fromState;
        this.type = type;
    }
}
