package org.evomaster.solver;

import org.evomaster.solver.smtlib.value.SMTLibValue;

import java.util.Map;

/**
 * Represents a satisfying solution produced by the Z3 solver: a mapping from
 * SMT-LIB variable/constant names to the values Z3 assigned to them.
 *
 * This is intentionally an abstract type (rather than a raw {@link Map}) so that
 * alternative solution representations can be introduced without changing callers.
 */
public abstract class Z3Solution {

    /**
     * @return the assignments of this solution, keyed by variable/constant name.
     */
    public abstract Map<String, SMTLibValue> getAssignments();

    /**
     * @param name the variable/constant name
     * @return the value assigned to the given name, or {@code null} if absent.
     */
    public SMTLibValue get(String name) {
        return getAssignments().get(name);
    }

    /**
     * @param name the variable/constant name
     * @return whether the given name has an assigned value in this solution.
     */
    public boolean containsKey(String name) {
        return getAssignments().containsKey(name);
    }

    /**
     * @return the number of assignments in this solution.
     */
    public int size() {
        return getAssignments().size();
    }
}
