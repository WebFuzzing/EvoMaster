package org.evomaster.client.java.instrumentation.cfg;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Per-method Control Flow Graph, represented as a set of basic blocks and edges between them.
 */
public class ControlFlowGraph {

    private final String className;     // bytecode name (eg java/lang/String)
    private final String methodName;    // eg "foo"
    private final String descriptor;    // eg (I)Z

    private final Map<Integer, BasicBlock> blocksById = new LinkedHashMap<>();
    private final Map<Integer, Integer> instructionIndexToBlockId = new LinkedHashMap<>();
    private final Map<Integer, Integer> instructionIndexToLineNumber = new LinkedHashMap<>();
    private final Map<Integer, java.util.List<Integer>> lineNumberToInstructionIndices = new LinkedHashMap<>();
    private final java.util.Set<Integer> branchInstructionIndices = new java.util.LinkedHashSet<>();
    private final Map<Integer, java.lang.Integer> instructionIndexToOpcode = new LinkedHashMap<>();
    private final Map<Integer, java.util.List<Integer>> lineNumberToBranchInstructionIndices = new LinkedHashMap<>();

    private Integer entryBlockId;

    public ControlFlowGraph(String className, String methodName, String descriptor) {
        this.className = Objects.requireNonNull(className);
        this.methodName = Objects.requireNonNull(methodName);
        this.descriptor = Objects.requireNonNull(descriptor);
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void addBlock(BasicBlock block) {
        blocksById.put(block.getId(), block);
        for (int i = block.getStartInstructionIndex(); i <= block.getEndInstructionIndex(); i++) {
            instructionIndexToBlockId.put(i, block.getId());
        }
        if (entryBlockId == null) {
            entryBlockId = block.getId();
        }
    }

    public Map<Integer, BasicBlock> getBlocksById() {
        return Collections.unmodifiableMap(blocksById);
    }

    public Integer getEntryBlockId() {
        return entryBlockId;
    }

    public Integer blockIdForInstructionIndex(int insnIndex) {
        return instructionIndexToBlockId.get(insnIndex);
    }

    public void addLineMapping(int instructionIndex, int lineNumber) {
        instructionIndexToLineNumber.put(instructionIndex, lineNumber);
        lineNumberToInstructionIndices.computeIfAbsent(lineNumber, k -> new java.util.ArrayList<>())
                .add(instructionIndex);
    }

    /**
     * Record the opcode for a given instruction index (non-pseudo).
     */
    public void addOpcodeMapping(int instructionIndex, int opcode) {
        instructionIndexToOpcode.put(instructionIndex, opcode);
    }

    /**
     * Mark an instruction index as a branch (conditional jump or switch).
     * Also indexes it under the corresponding source line if available.
     */
    public void addBranchInstructionIndex(int instructionIndex) {
        branchInstructionIndices.add(instructionIndex);
        Integer line = instructionIndexToLineNumber.get(instructionIndex);
        if (line != null) {
            lineNumberToBranchInstructionIndices
                    .computeIfAbsent(line, k -> new java.util.ArrayList<>())
                    .add(instructionIndex);
        }
    }

    public Map<Integer, Integer> getInstructionIndexToLineNumber() {
        return Collections.unmodifiableMap(instructionIndexToLineNumber);
    }

    public java.util.List<Integer> getInstructionIndicesForLine(int lineNumber) {
        java.util.List<Integer> list = lineNumberToInstructionIndices.get(lineNumber);
        return list == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(list);
    }

    public java.util.List<Integer> getBranchInstructionIndicesForLine(int lineNumber) {
        java.util.List<Integer> list = lineNumberToBranchInstructionIndices.get(lineNumber);
        return list == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(list);
    }

    public java.util.Set<Integer> getBranchInstructionIndices() {
        return java.util.Collections.unmodifiableSet(branchInstructionIndices);
    }

    public Map<Integer, Integer> getInstructionIndexToOpcode() {
        return java.util.Collections.unmodifiableMap(instructionIndexToOpcode);
    }
}


