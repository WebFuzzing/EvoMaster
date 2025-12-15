/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster.
 */
package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch;

import java.util.Objects;

public class ControlDependency {

    private final Branch branch;
    private final boolean branchExpressionValue;

    /**
     * <p>Constructor for ControlDependency.</p>
     *
     * @param branch                a {@link org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch} object.
     * @param branchExpressionValue a boolean.
     */
    public ControlDependency(Branch branch, boolean branchExpressionValue) {
        if (branch == null)
            throw new IllegalArgumentException(
                    "control dependencies for the root branch are not permitted (null)");

        this.branch = branch;
        this.branchExpressionValue = branchExpressionValue;
    }

    /**
     * <p>Getter for the field <code>branch</code>.</p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch} object.
     */
    public Branch getBranch() {
        return branch;
    }

    /**
     * <p>Getter for the field <code>branchExpressionValue</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getBranchExpressionValue() {
        return branchExpressionValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        String r = "CD " + branch;

        if (branchExpressionValue)
            r += " - TRUE";
        else
            r += " - FALSE";

        return r;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ControlDependency that = (ControlDependency) o;
        return branchExpressionValue == that.branchExpressionValue &&
                Objects.equals(branch, that.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branch, branchExpressionValue);
    }

}
