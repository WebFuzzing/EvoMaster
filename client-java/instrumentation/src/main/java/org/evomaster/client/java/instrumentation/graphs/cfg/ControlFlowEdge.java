/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster.
 */
package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch;
import org.jgrapht.graph.DefaultEdge;

public class ControlFlowEdge extends DefaultEdge {

    private ControlDependency cd;
    private boolean isExceptionEdge;

    /**
     * <p>Constructor for ControlFlowEdge.</p>
     */
    public ControlFlowEdge() {
        this.cd = null;
        this.isExceptionEdge = false;
    }

    /**
     * <p>Constructor for ControlFlowEdge.</p>
     *
     * @param isExceptionEdge a boolean.
     */
    public ControlFlowEdge(boolean isExceptionEdge) {
        this.isExceptionEdge = isExceptionEdge;
    }

    /**
     * <p>Constructor for ControlFlowEdge.</p>
     *
     * @param cd              a {@link org.evomaster.client.java.instrumentation.graphs.cfg.ControlDependency} object.
     * @param isExceptionEdge a boolean.
     */
    public ControlFlowEdge(ControlDependency cd, boolean isExceptionEdge) {
        this.cd = cd;
        this.isExceptionEdge = isExceptionEdge;
    }


    /**
     * Sort of a copy constructor
     *
     * @param clone a {@link org.evomaster.client.java.instrumentation.graphs.cfg.ControlFlowEdge} object.
     */
    public ControlFlowEdge(ControlFlowEdge clone) {
        if (clone != null) {
            this.cd = clone.cd;
            this.isExceptionEdge = clone.isExceptionEdge;
        }
    }

    /**
     * <p>getControlDependency</p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.ControlDependency} object.
     */
    public ControlDependency getControlDependency() {
        return cd;
    }

    /**
     * <p>hasControlDependency</p>
     *
     * @return a boolean.
     */
    public boolean hasControlDependency() {
        return cd != null;
    }

    /**
     * <p>getBranchInstruction</p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch} object.
     */
    public Branch getBranchInstruction() {
        if (cd == null)
            return null;

        return cd.getBranch();
    }

    /**
     * <p>isExceptionEdge</p>
     *
     * @return a boolean.
     */
    public boolean isExceptionEdge() {
        return isExceptionEdge;
    }

    /**
     * <p>getBranchExpressionValue</p>
     *
     * @return a boolean.
     */
    public boolean getBranchExpressionValue() {
        if (hasControlDependency())
            return cd.getBranchExpressionValue();

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String r = "";
        if (isExceptionEdge)
            r += "E ";
        if (cd != null)
            r += cd.toString();
        return r;
    }
}
