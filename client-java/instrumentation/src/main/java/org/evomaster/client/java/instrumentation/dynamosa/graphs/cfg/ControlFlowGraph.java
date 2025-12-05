/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.EvoMasterGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;


public abstract class ControlFlowGraph<V> extends
        EvoMasterGraph<V, ControlFlowEdge> {

    protected String className;
    protected String methodName;
    protected int access;

    /**
     * Creates a fresh and empty CFG for the given class and method
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @param access     a int.
     */
    protected ControlFlowGraph(String className, String methodName, int access) {
        super(ControlFlowEdge.class);

        if (className == null || methodName == null)
            throw new IllegalArgumentException("null given");

        this.className = className;
        this.methodName = methodName;
        this.access = access;
    }

    /**
     * Creates a CFG determined by the given jGraph for the given class and
     * method
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @param access     a int.
     * @param jGraph     a {@link org.jgrapht.graph.DefaultDirectedGraph} object.
     */
    protected ControlFlowGraph(String className, String methodName, int access,
                               DefaultDirectedGraph<V, ControlFlowEdge> jGraph) {
        super(jGraph, ControlFlowEdge.class);

        if (className == null || methodName == null)
            throw new IllegalArgumentException("null given");

        this.className = className;
        this.methodName = methodName;
        this.access = access;
    }

    /**
     * <p>leadsToNode</p>
     *
     * @param e a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ControlFlowEdge} object.
     * @param b a V object.
     * @return a boolean.
     */
    public boolean leadsToNode(ControlFlowEdge e, V b) {

        Set<V> handled = new HashSet<>();

        Queue<V> queue = new LinkedList<>();
        queue.add(getEdgeTarget(e));
        while (!queue.isEmpty()) {
            V current = queue.poll();
            if (handled.contains(current))
                continue;
            handled.add(current);

            for (V next : getChildren(current))
                if (next.equals(b))
                    return true;
                else
                    queue.add(next);
        }

        return false;
    }

    // /**
    // * Can be used to retrieve a Branch contained in this CFG identified by
    // it's
    // * branchId
    // *
    // * If no such branch exists in this CFG, null is returned
    // */
    // public abstract BytecodeInstruction getBranch(int branchId);

    /**
     * Can be used to retrieve an instruction contained in this CFG identified
     * by it's instructionId
     * <p>
     * If no such instruction exists in this CFG, null is returned
     *
     * @param instructionId a int.
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public abstract BytecodeInstruction getInstruction(int instructionId);

    /**
     * Determines, whether a given instruction is contained in this CFG
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a boolean.
     */
    public abstract boolean containsInstruction(BytecodeInstruction instruction);

    /**
     * <p>determineEntryPoint</p>
     *
     * @return a V object.
     */
    public V determineEntryPoint() {
        Set<V> candidates = determineEntryPoints();

        if (candidates.size() > 1)
            throw new IllegalStateException(
                    "expect CFG of a method to contain at most one instruction with no parent in "
                            + methodName);

        for (V instruction : candidates)
            return instruction;

        // there was a back loop to the first instruction within this CFG, so no
        // candidate
        // can also happen in empty methods
        return null;
    }

    /**
     * <p>Getter for the field <code>className</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getClassName() {
        return className;
    }

    /**
     * <p>Getter for the field <code>methodName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * <p>Getter for the field <code>access</code>.</p>
     *
     * @return a int.
     */
    public int getMethodAccess() {
        return access;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return methodName + " " + getCFGType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String dotSubFolder() {
        return toFileString(className) + "/" + getCFGType() + "/";
    }

    /**
     * <p>getCFGType</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public abstract String getCFGType();
}
