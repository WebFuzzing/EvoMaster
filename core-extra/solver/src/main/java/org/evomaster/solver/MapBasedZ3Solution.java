package org.evomaster.solver;

import org.evomaster.solver.smtlib.value.SMTLibValue;

import java.util.Map;

/**
 * A {@link Z3Solution} backed by a plain map of variable/constant assignments,
 * as produced by parsing Z3's textual output.
 */
public class MapBasedZ3Solution extends Z3Solution {

    private final Map<String, SMTLibValue> assignments;

    public MapBasedZ3Solution(Map<String, SMTLibValue> assignments) {
        this.assignments = assignments;
    }

    @Override
    public Map<String, SMTLibValue> getAssignments() {
        return assignments;
    }
}
