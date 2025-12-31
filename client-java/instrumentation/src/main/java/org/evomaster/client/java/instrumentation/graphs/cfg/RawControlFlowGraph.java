/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster.
 */
package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.evomaster.client.java.instrumentation.graphs.cfg.branch.BranchPool;

import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;

import static java.util.Comparator.comparingInt;

/**
 * Represents the complete CFG of a method
 * <p>
 * Essentially this is a graph containing all BytecodeInstrucions of a method as
 * nodes. From each such instruction there is an edge to each possible
 * instruction the control flow can reach immediately after that instruction.
 *
 */
public class RawControlFlowGraph extends ControlFlowGraph<BytecodeInstruction> {

    private final ClassLoader classLoader;

    /**
     * @return the classLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * <p>
     * Constructor for RawControlFlowGraph.
     * </p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @param access     a int.
     */
    public RawControlFlowGraph(ClassLoader classLoader, String className,
                               String methodName, int access) {
        super(className, methodName, access);
        this.classLoader = classLoader;
        SimpleLogger.info("Creating new RawCFG for " + className + "." + methodName + ": " + this.vertexCount());
    }

    // inherited from ControlFlowGraph

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsInstruction(BytecodeInstruction instruction) {

        return containsVertex(instruction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BytecodeInstruction getInstruction(int instructionId) {
        return vertexSet().stream()
                .filter(v -> v.getInstructionId() == instructionId)
                .findFirst()
                .orElse(null);
    }

    /**
     * <p>
     * addEdge
     * </p>
     *
     * @param src             a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction} object.
     * @param target          a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction} object.
     * @param isExceptionEdge a boolean.
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.ControlFlowEdge} object.
     */
    protected ControlFlowEdge addEdge(BytecodeInstruction src,
                                      BytecodeInstruction target, boolean isExceptionEdge) {

        if (BranchPool.getInstance(classLoader).isKnownAsBranch(src) && src.isBranch()) {
                return addBranchEdge(src, target, isExceptionEdge);
        }

        return addUnlabeledEdge(src, target, isExceptionEdge);
    }

    private ControlFlowEdge addUnlabeledEdge(BytecodeInstruction src,
                                             BytecodeInstruction target, boolean isExceptionEdge) {

        return internalAddEdge(src, target, new ControlFlowEdge(isExceptionEdge));
    }

    private ControlFlowEdge addBranchEdge(BytecodeInstruction src,
                                          BytecodeInstruction target, boolean isExceptionEdge) {

        boolean isJumping = !isNonJumpingEdge(src, target);
        ControlDependency cd = new ControlDependency(src.toBranch(), isJumping);

        ControlFlowEdge e = new ControlFlowEdge(cd, isExceptionEdge);

        return internalAddEdge(src, target, e);
    }

    private ControlFlowEdge internalAddEdge(BytecodeInstruction src,
                                            BytecodeInstruction target, ControlFlowEdge e) {

        if (!super.addEdge(src, target, e)) {
            // Edge already exists - retrieve the existing one
            e = super.getEdge(src, target);
            if (e == null) {
                throw new IllegalStateException(
                        "internal graph error - completely unexpected");
            }
        }

        return e;
    }

    private boolean isNonJumpingEdge(BytecodeInstruction src, // TODO move to
                                     // ControlFlowGraph
                                     // and implement
                                     // analog method
                                     // in ActualCFG
                                     BytecodeInstruction dst) {

        return Math.abs(src.getInstructionId() - dst.getInstructionId()) == 1;
    }

    // functionality used to create ActualControlFlowGraph

    /**
     * <p>
     * determineBasicBlockFor
     * </p>
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction} object.
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BasicBlock} object.
     */
    public BasicBlock determineBasicBlockFor(BytecodeInstruction instruction) {
        if (instruction == null) {
            throw new IllegalArgumentException("null given");
        }

        List<BytecodeInstruction> blockNodes = new ArrayList<>();
        blockNodes.add(instruction);

        Set<BytecodeInstruction> handledChildren = new HashSet<>();
        Set<BytecodeInstruction> handledParents = new HashSet<>();

        Queue<BytecodeInstruction> queue = new LinkedList<>();
        queue.add(instruction);
        while (!queue.isEmpty()) {
            BytecodeInstruction current = queue.poll();

            // add child to queue
            if (outDegreeOf(current) == 1) {
                for (BytecodeInstruction child : getChildren(current)) {
                    // this must be only one edge if inDegree was 1

                    if (blockNodes.contains(child)) {
                        continue;
                    }

                    if (handledChildren.contains(child)) {
                        continue;
                    }
                    handledChildren.add(child);

                    if (inDegreeOf(child) < 2) {
                        // insert child right after current
                        blockNodes.add(blockNodes.indexOf(current) + 1, child);
                        queue.add(child);
                    }
                }
            }

            // add parent to queue
            if (inDegreeOf(current) == 1) {
                for (BytecodeInstruction parent : getParents(current)) {
                    // this must be only one edge if outDegree was 1

                    if (blockNodes.contains(parent)) {
                        continue;
                    }

                    if (handledParents.contains(parent)) {
                        continue;
                    }
                    handledParents.add(parent);

                    if (outDegreeOf(parent) < 2) {
                        // insert parent right before current
                        blockNodes.add(blockNodes.indexOf(current), parent);
                        queue.add(parent);
                    }
                }
            }
        }

        return new BasicBlock(classLoader, className, methodName, blockNodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BytecodeInstruction determineEntryPoint() {

        BytecodeInstruction noParent = super.determineEntryPoint();
        if (noParent != null)
            return noParent;

        // copied from ControlFlowGraph.determineEntryPoint():
        // there was a back loop to the first instruction within this CFG, so no
        // candidate

        return getInstructionWithSmallestId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<BytecodeInstruction> determineExitPoints() {

        Set<BytecodeInstruction> r = super.determineExitPoints();

        // if the last instruction loops back to a previous instruction there is
        // no node without a child, so just take the last byteCode instruction

        if (r.isEmpty())
            r.add(getInstructionWithBiggestId());

        return r;

    }

    /**
     * <p>
     * determineBranches
     * </p>
     *
     * @return Set containing all nodes with out degree > 1
     */
    public Set<BytecodeInstruction> determineBranches() {
        Set<BytecodeInstruction> r = new HashSet<>();
        for (BytecodeInstruction instruction : vertexSet())
            if (outDegreeOf(instruction) > 1)
                r.add(instruction);
        return r;
    }

    /**
     * <p>
     * determineJoins
     * </p>
     *
     * @return Set containing all nodes with in degree > 1
     */
    public Set<BytecodeInstruction> determineJoins() {
        Set<BytecodeInstruction> r = new HashSet<>();
        for (BytecodeInstruction instruction : vertexSet())
            if (inDegreeOf(instruction) > 1)
                r.add(instruction);
        return r;
    }

    /**
     * <p>
     * getInstructionWithSmallestId
     * </p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction getInstructionWithSmallestId() {
        return vertexSet().stream()
                .min(comparingInt(BytecodeInstruction::getInstructionId))
                .orElse(null);
    }

    /**
     * <p>
     * getInstructionWithBiggestId
     * </p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction getInstructionWithBiggestId() {
        return vertexSet().stream()
                .max(comparingInt(BytecodeInstruction::getInstructionId))
                .orElse(null);
    }

    /**
     * In some cases there can be isolated nodes within a CFG. For example in an
     * completely empty try-catch-finally. Since these nodes are not reachable
     * but cause trouble when determining the entry point of a CFG they get
     * removed.
     *
     * @return a int.
     */
    public int removeIsolatedNodes() {
        Set<BytecodeInstruction> candidates = determineEntryPoints();

        int removed = 0;
        if (candidates.size() > 1) {

            for (BytecodeInstruction instruction : candidates) {
                if (outDegreeOf(instruction) == 0 && graph.removeVertex(instruction)) {
                    removed++;
                    BytecodeInstructionPool.getInstance(classLoader).forgetInstruction(instruction);
                }
            }

        }
        return removed;
    }

    // miscellaneous

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (ControlFlowEdge e : graph.edgeSet()) {
            sb.append(graph.getEdgeSource(e) + " -> " + graph.getEdgeTarget(e));
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCFGType() {
        return "RCFG";
    }

    /**
     * {@inheritDoc}
     */
    public boolean addVertex(BytecodeInstruction ins) {
        return super.addVertex(ins);
    }
}
