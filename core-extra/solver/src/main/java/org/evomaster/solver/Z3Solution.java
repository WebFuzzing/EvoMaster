package org.evomaster.solver;

import org.evomaster.solver.smtlib.value.SMTLibValue;

import java.util.Map;

/**
 * Represents a satisfying solution produced by the Z3 solver: a mapping from
 * SMT-LIB variable/constant names to the values Z3 assigned to them.
 */
public class Z3Solution {

    private final Map<String, SMTLibValue> assignments;

    public Z3Solution(Map<String, SMTLibValue> assignments) {
        this.assignments = assignments;
    }

    /**
     * @return the assignments of this solution, keyed by variable/constant name.
     */
    public Map<String, SMTLibValue> getAssignments() {
        return assignments;
    }

    /**
     * @param name the variable/constant name
     * @return the value assigned to the given name, or {@code null} if absent.
     */
    public SMTLibValue get(String name) {
        return assignments.get(name);
    }

    /**
     * @param name the variable/constant name
     * @return whether the given name has an assigned value in this solution.
     */
    public boolean containsKey(String name) {
        return assignments.containsKey(name);
    }

    /**
     * @return the number of assignments in this solution.
     */
    public int size() {
        return assignments.size();
    }

    /**
     * @return whether this solution has no assignments.
     */
    public boolean isEmpty() {
        return assignments.isEmpty();
    }
}
