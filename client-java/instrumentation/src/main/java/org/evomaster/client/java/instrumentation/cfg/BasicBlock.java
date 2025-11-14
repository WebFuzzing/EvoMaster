package org.evomaster.client.java.instrumentation.cfg;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A basic block groups a consecutive range of bytecode instructions with a single entry and single exit,
 * and tracks control-flow successors and predecessors at block granularity.
 */
public class BasicBlock {

    private final int id;
    private final int startInstructionIndex;
    private final int endInstructionIndex;

    private final Set<Integer> successorBlockIds = new LinkedHashSet<>();
    private final Set<Integer> predecessorBlockIds = new LinkedHashSet<>();

    public BasicBlock(int id, int startInstructionIndex, int endInstructionIndex) {
        if (startInstructionIndex < 0 || endInstructionIndex < startInstructionIndex) {
            throw new IllegalArgumentException("Invalid instruction index range for basic block");
        }
        this.id = id;
        this.startInstructionIndex = startInstructionIndex;
        this.endInstructionIndex = endInstructionIndex;
    }

    public int getId() {
        return id;
    }

    public int getStartInstructionIndex() {
        return startInstructionIndex;
    }

    public int getEndInstructionIndex() {
        return endInstructionIndex;
    }

    public void addSuccessor(int blockId) {
        successorBlockIds.add(blockId);
    }

    public void addPredecessor(int blockId) {
        predecessorBlockIds.add(blockId);
    }

    public Set<Integer> getSuccessorBlockIds() {
        return Collections.unmodifiableSet(successorBlockIds);
    }

    public Set<Integer> getPredecessorBlockIds() {
        return Collections.unmodifiableSet(predecessorBlockIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicBlock)) return false;
        BasicBlock that = (BasicBlock) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BasicBlock{" +
                "id=" + id +
                ", start=" + startInstructionIndex +
                ", end=" + endInstructionIndex +
                ", succ=" + successorBlockIds +
                ", pred=" + predecessorBlockIds +
                '}';
    }
}


