/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.utils.SimpleLogger;

import java.util.HashSet;
import java.util.Set;

public class ActualControlFlowGraph extends ControlFlowGraph<BasicBlock> {

    private RawControlFlowGraph rawGraph;

    private BytecodeInstruction entryPoint;
    private Set<BytecodeInstruction> exitPoints;
    private Set<BytecodeInstruction> branches;
    private Set<BytecodeInstruction> branchTargets;
    private Set<BytecodeInstruction> joins;
    private Set<BytecodeInstruction> joinSources;

    /**
     * <p>
     * Constructor for ActualControlFlowGraph.
     * </p>
     *
     * @param rawGraph a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.RawControlFlowGraph} object.
     */
    public ActualControlFlowGraph(RawControlFlowGraph rawGraph) {
        super(rawGraph.getClassName(), rawGraph.getMethodName(),
                rawGraph.getMethodAccess());

        this.rawGraph = rawGraph;

        fillSets();
        computeGraph();
    }

    /**
     * <p>
     * Constructor for ActualControlFlowGraph.
     * </p>
     *
     * @param toRevert a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ActualControlFlowGraph}
     *                 object.
     */
    protected ActualControlFlowGraph(ActualControlFlowGraph toRevert) {
        super(toRevert.className, toRevert.methodName, toRevert.access,
                toRevert.computeReverseJGraph());
    }

    /**
     * <p>
     * computeReverseCFG
     * </p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ActualControlFlowGraph} object.
     */
    public ActualControlFlowGraph computeReverseCFG() {
        // TODO: this must be possible to "pre implement" in EvoMasterGraph for
        // all sub class of EvoMasterGraph
        return new ActualControlFlowGraph(this);
    }

    // initialization

    private void fillSets() {

        setEntryPoint(rawGraph.determineEntryPoint());
        setExitPoints(rawGraph.determineExitPoints());

        setBranches(rawGraph.determineBranches());
        setBranchTargets();
        setJoins(rawGraph.determineJoins());
        setJoinSources();
    }

    private void setEntryPoint(BytecodeInstruction entryPoint) {
        if (entryPoint == null)
            throw new IllegalArgumentException("null given");
        if (!belongsToMethod(entryPoint))
            throw new IllegalArgumentException(
                    "entry point does not belong to this CFGs method");
        this.entryPoint = entryPoint;
    }

    private void setExitPoints(Set<BytecodeInstruction> exitPoints) {
        if (exitPoints == null)
            throw new IllegalArgumentException("null given");

        this.exitPoints = new HashSet<>();

        for (BytecodeInstruction exitPoint : exitPoints) {
            if (!belongsToMethod(exitPoint))
                throw new IllegalArgumentException(
                        "exit point does not belong to this CFGs method");
            if (!exitPoint.canBeExitPoint())
                throw new IllegalArgumentException(
                        "unexpected exitPoint byteCode instruction type: "
                                + exitPoint.getInstructionType());

            this.exitPoints.add(exitPoint);
        }
    }

    private void setJoins(Set<BytecodeInstruction> joins) {
        if (joins == null)
            throw new IllegalArgumentException("null given");

        this.joins = new HashSet<>();

        for (BytecodeInstruction join : joins) {
            if (!belongsToMethod(join))
                throw new IllegalArgumentException(
                        "join does not belong to this CFGs method");

            this.joins.add(join);
        }
    }

    private void setJoinSources() {
        if (joins == null)
            throw new IllegalStateException(
                    "expect joins to be set before setting of joinSources");
        if (rawGraph == null)
            throw new IllegalArgumentException("null given");

        this.joinSources = new HashSet<>();

        for (BytecodeInstruction join : joins)
            for (ControlFlowEdge joinEdge : rawGraph.incomingEdgesOf(join))
                joinSources.add(rawGraph.getEdgeSource(joinEdge));
    }

    private void setBranches(Set<BytecodeInstruction> branches) {
        if (branches == null)
            throw new IllegalArgumentException("null given");

        this.branches = new HashSet<>();

        for (BytecodeInstruction branch : branches) {
            if (!belongsToMethod(branch))
                throw new IllegalArgumentException(
                        "branch does not belong to this CFGs method");

            this.branches.add(branch);
        }
    }

    private void setBranchTargets() {
        if (branches == null)
            throw new IllegalStateException(
                    "expect branches to be set before setting of branchTargets");
        if (rawGraph == null)
            throw new IllegalArgumentException("null given");

        this.branchTargets = new HashSet<>();

        for (BytecodeInstruction branch : branches)
            for (ControlFlowEdge branchEdge : rawGraph.outgoingEdgesOf(branch))
                branchTargets.add(rawGraph.getEdgeTarget(branchEdge));
    }

    private Set<BytecodeInstruction> getInitiallyKnownInstructions() {
        Set<BytecodeInstruction> r = new HashSet<>();
        r.add(entryPoint);
        r.addAll(exitPoints);
        r.addAll(branches);
        r.addAll(branchTargets);
        r.addAll(joins);
        r.addAll(joinSources);

        return r;
    }

    // compute actual CFG from RawControlFlowGraph

    private void computeGraph() {

        computeNodes();
        computeEdges();

        addAuxiliaryBlocks();
    }

    private void addAuxiliaryBlocks() {

        // TODO clean up mess: exit-/entry- POINTs versus BLOCKs

        EntryBlock entry = new EntryBlock(className, methodName);
        ExitBlock exit = new ExitBlock(className, methodName);

        addBlock(entry);
        addBlock(exit);
        addEdge(entry, exit);
        addEdge(entry, this.entryPoint.getBasicBlock());
        for (BytecodeInstruction exitPoint : this.exitPoints) {
            addEdge(exitPoint.getBasicBlock(), exit);
        }
    }

    private void computeNodes() {

        Set<BytecodeInstruction> nodes = getInitiallyKnownInstructions();

        for (BytecodeInstruction node : nodes) {
            if (knowsInstruction(node))
                continue;

            BasicBlock nodeBlock = rawGraph.determineBasicBlockFor(node);
            addBlock(nodeBlock);
        }

        SimpleLogger.debug(vertexCount() + " BasicBlocks");
    }

    private void computeEdges() {

        for (BasicBlock block : vertexSet()) {

            computeIncomingEdgesFor(block);
            computeOutgoingEdgesFor(block);
        }

        SimpleLogger.debug(edgeCount() + " ControlFlowEdges");
    }

    private void computeIncomingEdgesFor(BasicBlock block) {

        if (isEntryPoint(block))
            return;

        BytecodeInstruction blockStart = block.getFirstInstruction();
        Set<ControlFlowEdge> rawIncomings = rawGraph.incomingEdgesOf(blockStart);
        for (ControlFlowEdge rawIncoming : rawIncomings) {
            BytecodeInstruction incomingStart = rawGraph.getEdgeSource(rawIncoming);
            addRawEdge(incomingStart, block, rawIncoming);
        }
    }

    private void computeOutgoingEdgesFor(BasicBlock block) {

        if (isExitPoint(block))
            return;

        BytecodeInstruction blockEnd = block.getLastInstruction();

        Set<ControlFlowEdge> rawOutgoings = rawGraph.outgoingEdgesOf(blockEnd);
        for (ControlFlowEdge rawOutgoing : rawOutgoings) {
            BytecodeInstruction outgoingEnd = rawGraph.getEdgeTarget(rawOutgoing);
            addRawEdge(block, outgoingEnd, rawOutgoing);
        }
    }

    // internal graph handling

    /**
     * <p>
     * addBlock
     * </p>
     *
     * @param nodeBlock a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     */
    protected void addBlock(BasicBlock nodeBlock) {
        if (nodeBlock == null)
            throw new IllegalArgumentException("null given");

        SimpleLogger.debug("Adding block: " + nodeBlock.getName());

        if (containsVertex(nodeBlock))
            throw new IllegalArgumentException("block already added before");

        if (!addVertex(nodeBlock))
            throw new IllegalStateException(
                    "internal error while addind basic block to CFG");


        if (!containsVertex(nodeBlock))
            throw new IllegalStateException(
                    "expect graph to contain the given block on returning of addBlock()");

        SimpleLogger.debug(".. succeeded. nodeCount: " + vertexCount());
    }

    /**
     * <p>
     * addRawEdge
     * </p>
     *
     * @param src      a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @param target   a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     * @param origEdge a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ControlFlowEdge} object.
     */
    protected void addRawEdge(BytecodeInstruction src, BasicBlock target,
                              ControlFlowEdge origEdge) {
        BasicBlock srcBlock = src.getBasicBlock();
        if (srcBlock == null)
            throw new IllegalStateException(
                    "when adding an edge to a CFG it is expected to know both the src- and the target-instruction");

        addRawEdge(srcBlock, target, origEdge);
    }

    /**
     * <p>
     * addRawEdge
     * </p>
     *
     * @param src      a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     * @param target   a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @param origEdge a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ControlFlowEdge} object.
     */
    protected void addRawEdge(BasicBlock src, BytecodeInstruction target,
                              ControlFlowEdge origEdge) {
        BasicBlock targetBlock = target.getBasicBlock();
        if (targetBlock == null)
            throw new IllegalStateException(
                    "when adding an edge to a CFG it is expected to know both the src- and the target-instruction");

        addRawEdge(src, targetBlock, origEdge);
    }

    /**
     * <p>
     * addRawEdge
     * </p>
     *
     * @param src      a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     * @param target   a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     * @param origEdge a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ControlFlowEdge} object.
     */
    protected void addRawEdge(BasicBlock src, BasicBlock target, ControlFlowEdge origEdge) {
        if (src == null || target == null)
            throw new IllegalArgumentException("null given");

        SimpleLogger.debug("Adding edge from " + src.getName() + " to " + target.getName());

        if (containsEdge(src, target)) {
            SimpleLogger.debug("edge already contained in CFG");
            // sanity check
            ControlFlowEdge current = getEdge(src, target);
            if (current == null)
                throw new IllegalStateException(
                        "expect getEdge() not to return null on parameters on which containsEdge() retruned true");
            if (current.getBranchExpressionValue()
                    && !origEdge.getBranchExpressionValue())
                throw new IllegalStateException(
                        "if this rawEdge was handled before i expect the old edge to have same branchExpressionValue set");
            if (current.getBranchInstruction() == null) {
                if (origEdge.getBranchInstruction() != null)
                    throw new IllegalStateException(
                            "if this rawEdge was handled before i expect the old edge to have same branchInstruction set");

            } else if (origEdge.getBranchInstruction() == null
                    || !current.getBranchInstruction().equals(origEdge.getBranchInstruction()))
                throw new IllegalStateException(
                        "if this rawEdge was handled before i expect the old edge to have same branchInstruction set");

            return;
        }

        ControlFlowEdge e = new ControlFlowEdge(origEdge);
        if (!super.addEdge(src, target, e))
            throw new IllegalStateException("internal error while adding edge to CFG");

        SimpleLogger.debug(".. succeeded, edgeCount: " + edgeCount());
    }

    // convenience methods to switch between BytecodeInstructons and BasicBlocks

    /**
     * If the given instruction is known to this graph, the BasicBlock holding
     * that instruction is returned. Otherwise null will be returned.
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     */
    public BasicBlock getBlockOf(BytecodeInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("null given");

        if (instruction.hasBasicBlockSet())
            return instruction.getBasicBlock();

        for (BasicBlock block : vertexSet())
            if (block.containsInstruction(instruction)) {
                instruction.setBasicBlock(block);
                return block;
            }

        SimpleLogger.debug("unknown instruction " + instruction);
        return null;
    }

    /**
     * Checks whether this graph knows the given instruction. That is there is a
     * BasicBlock in this graph's vertexSet containing the given instruction.
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a boolean.
     */
    public boolean knowsInstruction(BytecodeInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("null given");

        if (instruction.hasBasicBlockSet())
            return containsVertex(instruction.getBasicBlock());

        for (BasicBlock block : vertexSet())
            if (block.containsInstruction(instruction))
                return true;

        return false;
    }

    /**
     * <p>
     * getDistance
     * </p>
     *
     * @param v1 a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @param v2 a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a int.
     */
    /**
     * <p>
     * isEntryPoint
     * </p>
     *
     * @param block a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     * @return a boolean.
     */
    private boolean isEntryPoint(BasicBlock block) {
        if (block == null)
            throw new IllegalArgumentException("null given");

        return block.containsInstruction(entryPoint);
    }

    /**
     * <p>
     * isExitPoint
     * </p>
     *
     * @param block a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     * @return a boolean.
     */
    private boolean isExitPoint(BasicBlock block) {
        if (block == null)
            throw new IllegalArgumentException("null given");

        for (BytecodeInstruction exitPoint : exitPoints)
            if (block.containsInstruction(exitPoint)) {
                return true;
            }

        return false;
    }

    /**
     * <p>
     * belongsToMethod
     * </p>
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a boolean.
     */
    private boolean belongsToMethod(BytecodeInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("null given");

        if (!className.equals(instruction.getClassName()))
            return false;
        return methodName.equals(instruction.getMethodName());
    }

    // inherited from ControlFlowGraph

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsInstruction(BytecodeInstruction v) {
        if (v == null)
            return false;

        for (BasicBlock block : vertexSet())
            if (block.containsInstruction(v))
                return true;

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BytecodeInstruction getInstruction(int instructionId) {

        BytecodeInstruction searchedFor = BytecodeInstructionPool.getInstance(rawGraph.getClassLoader()).getInstruction(className,
                methodName,
                instructionId);

        if (containsInstruction(searchedFor))
            return searchedFor;

        return null;
    }

    /**
     * <p>
     * Getter for the field <code>branches</code>.
     * </p>
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<BytecodeInstruction> getBranches() {
        return new HashSet<>(branches);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCFGType() {
        return "ACFG";
    }
}
