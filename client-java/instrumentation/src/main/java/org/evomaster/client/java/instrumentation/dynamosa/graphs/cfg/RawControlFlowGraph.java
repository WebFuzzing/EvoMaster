/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch.Branch;
import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch.BranchPool;

import org.objectweb.asm.tree.LabelNode;
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
 * @author Andre Mis
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

    // @Override
    // public BytecodeInstruction getBranch(int branchId) {
    // for (BytecodeInstruction v : vertexSet()) {
    // if (v.isBranch() && v.getControlDependentBranchId() == branchId) {
    // return v;
    // }
    // }
    // return null;
    // }

    /**
     * <p>
     * addEdge
     * </p>
     *
     * @param src             a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @param target          a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @param isExceptionEdge a boolean.
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ControlFlowEdge} object.
     */
    protected ControlFlowEdge addEdge(BytecodeInstruction src,
                                      BytecodeInstruction target, boolean isExceptionEdge) {

        SimpleLogger.debug("Adding edge to RawCFG of " + className + "." + methodName + ": " + this.vertexCount());

        if (BranchPool.getInstance(classLoader).isKnownAsBranch(src))
            if (src.isBranch())
                return addBranchEdge(src, target, isExceptionEdge);
            else if (src.isSwitch())
                return addSwitchBranchEdge(src, target, isExceptionEdge);

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

    private ControlFlowEdge addSwitchBranchEdge(BytecodeInstruction src,
                                                BytecodeInstruction target, boolean isExceptionEdge) {
        if (!target.isLabel())
            throw new IllegalStateException(
                    "expect control flow edges from switch statements to always target labelNodes");

        LabelNode label = (LabelNode) target.getASMNode();

        List<Branch> switchCaseBranches = BranchPool.getInstance(classLoader).getBranchForLabel(label);

        if (switchCaseBranches == null) {
            SimpleLogger.debug("not a switch case label: " + label.toString() + " "
                    + target);
            return internalAddEdge(src, target, new ControlFlowEdge(isExceptionEdge));
        }
        // throw new IllegalStateException(
        // "expect BranchPool to contain a Branch for each switch-case-label"+src.toString()+" to "+target.toString());

        // TODO there is an inconsistency when it comes to switches with
        // empty case: blocks. they do not have their own label, so there
        // can be multiple ControlFlowEdges from the SWITCH instruction to
        // one LabelNode.
        // But currently our RawCFG does not permit multiple edges between
        // two nodes

        for (Branch switchCaseBranch : switchCaseBranches) {

            // TODO n^2
            Set<ControlFlowEdge> soFar = incomingEdgesOf(target);
            boolean handled = false;
            for (ControlFlowEdge old : soFar)
                if (switchCaseBranch.equals(old.getBranchInstruction()))
                    handled = true;

            if (handled)
                continue;
            /*
             * previous try to add fake intermediate nodes for each empty case
             * block to help the CDG - unsuccessful:
             * if(switchCaseBranches.size()>1) { // // e = new
             * ControlFlowEdge(isExceptionEdge); //
             * e.setBranchInstruction(switchCaseBranch); //
             * e.setBranchExpressionValue(true); // BytecodeInstruction
             * fakeInstruction =
             * BytecodeInstructionPool.createFakeInstruction(className
             * ,methodName); // addVertex(fakeInstruction); //
             * internalAddEdge(src,fakeInstruction,e); // // e = new
             * ControlFlowEdge(isExceptionEdge); //
             * e.setBranchInstruction(switchCaseBranch); //
             * e.setBranchExpressionValue(true); // // e =
             * internalAddEdge(fakeInstruction,target,e); // } else {
             */

            ControlDependency cd = new ControlDependency(switchCaseBranch, true);
            ControlFlowEdge e = new ControlFlowEdge(cd, isExceptionEdge);

            e = internalAddEdge(src, target, e);

        }

        return new ControlFlowEdge(isExceptionEdge);
    }

    private ControlFlowEdge internalAddEdge(BytecodeInstruction src,
                                            BytecodeInstruction target, ControlFlowEdge e) {

        if (!super.addEdge(src, target, e)) {
            // TODO find out why this still happens
            SimpleLogger.debug("unable to add edge from " + src.toString() + " to "
                    + target.toString() + " into the rawCFG of " + getMethodName());
            e = super.getEdge(src, target);
            if (e == null)
                throw new IllegalStateException(
                        "internal graph error - completely unexpected");
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
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     */
    public BasicBlock determineBasicBlockFor(BytecodeInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("null given");

        // TODO clean this up

        SimpleLogger.debug("creating basic block for " + instruction);

        List<BytecodeInstruction> blockNodes = new ArrayList<>();
        blockNodes.add(instruction);

        Set<BytecodeInstruction> handledChildren = new HashSet<>();
        Set<BytecodeInstruction> handledParents = new HashSet<>();

        Queue<BytecodeInstruction> queue = new LinkedList<>();
        queue.add(instruction);
        while (!queue.isEmpty()) {
            BytecodeInstruction current = queue.poll();
            SimpleLogger.debug("handling " + current.toString());

            // add child to queue
            if (outDegreeOf(current) == 1)
                for (BytecodeInstruction child : getChildren(current)) {
                    // this must be only one edge if inDegree was 1

                    if (blockNodes.contains(child))
                        continue;

                    if (handledChildren.contains(child))
                        continue;
                    handledChildren.add(child);

                    if (inDegreeOf(child) < 2) {
                        // insert child right after current
                        // ... always thought ArrayList had insertBefore() and
                        // insertAfter() methods ... well
                        blockNodes.add(blockNodes.indexOf(current) + 1, child);

                        SimpleLogger.debug("  added child to queue: " + child.toString());
                        queue.add(child);
                    }
                }

            // add parent to queue
            if (inDegreeOf(current) == 1)
                for (BytecodeInstruction parent : getParents(current)) {
                    // this must be only one edge if outDegree was 1

                    if (blockNodes.contains(parent))
                        continue;

                    if (handledParents.contains(parent))
                        continue;
                    handledParents.add(parent);

                    if (outDegreeOf(parent) < 2) {
                        // insert parent right before current
                        blockNodes.add(blockNodes.indexOf(current), parent);

                        SimpleLogger.debug("  added parent to queue: " + parent.toString());
                        queue.add(parent);
                    }
                }
        }

        BasicBlock r = new BasicBlock(classLoader, className, methodName, blockNodes);

        SimpleLogger.debug("created nodeBlock: " + r);
        return r;
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
     * getInstructionWithSmallestId
     * </p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
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
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
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

    // control distance functionality

    /**
     * Returns the Set of BytecodeInstructions that can potentially be executed
     * from entering the method of this CFG until the given BytecodeInstruction
     * is reached.
     *
     * @param v a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a {@link java.util.Set} object.
     */
    public Set<BytecodeInstruction> getPreviousInstructionsInMethod(BytecodeInstruction v) {
        Set<BytecodeInstruction> visited = new HashSet<>();
        PriorityQueue<BytecodeInstruction> queue = new PriorityQueue<>(
                graph.vertexSet().size(), new BytecodeInstructionIdComparator());
        queue.add(v);
        while (queue.peek() != null) {
            BytecodeInstruction current = queue.poll();
            if (visited.contains(current))
                continue;
            Set<ControlFlowEdge> incomingEdges = graph.incomingEdgesOf(current);
            for (ControlFlowEdge incomingEdge : incomingEdges) {
                BytecodeInstruction source = graph.getEdgeSource(incomingEdge);
                if (source.getInstructionId() >= current.getInstructionId())
                    continue;
                queue.add(source);
            }
            visited.add(current);
        }
        return visited;
    }

    /**
     * Returns the Set of BytecodeInstructions that can potentially be executed
     * from passing the given BytecodeInstruction until the end of the method of
     * this CFG is reached.
     *
     * @param v a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a {@link java.util.Set} object.
     */
    public Set<BytecodeInstruction> getLaterInstructionsInMethod(BytecodeInstruction v) {
        Set<BytecodeInstruction> visited = new HashSet<>();
        Comparator<BytecodeInstruction> reverseComp = new BytecodeInstructionIdComparator().reversed();
        PriorityQueue<BytecodeInstruction> queue = new PriorityQueue<>(
                graph.vertexSet().size(), reverseComp);
        queue.add(v);
        while (queue.peek() != null) {
            BytecodeInstruction current = queue.poll();
            if (visited.contains(current))
                continue;
            Set<ControlFlowEdge> outgoingEdges = graph.outgoingEdgesOf(current);
            for (ControlFlowEdge outgoingEdge : outgoingEdges) {
                BytecodeInstruction target = graph.getEdgeTarget(outgoingEdge);
                if (target.getInstructionId() < current.getInstructionId())
                    continue;
                queue.add(target);
            }
            visited.add(current);
        }
        return visited;
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
