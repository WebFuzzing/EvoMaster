package org.evomaster.client.java.instrumentation.cfg;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Builds Control Flow Graphs from ASM ClassNode/MethodNode IR.
 * This focuses on structural control flow (sequential flow, conditional/unconditional jumps, switches).
 * Exception handlers can be added later.
 */
public class CFGGenerator {

    /**
     * Build CFGs for each eligible method in the given class and register them in CFGRecorder.
     */
    public static void computeAndRegister(ClassNode classNode) {
        for (Object m : classNode.methods) {
            MethodNode method = (MethodNode) m;
            // skip class initializer
            if ("<clinit>".equals(method.name)) {
                continue;
            }
            ControlFlowGraph cfg = compute(classNode.name, method);
            if (cfg != null) {
                CFGRecorder.register(cfg);
            }
        }
    }

    /**
     * Build a per-method CFG.
     */
    public static ControlFlowGraph compute(String classBytecodeName, MethodNode method) {
        RawBuildResult raw = buildRawGraph(classBytecodeName, method);
        if (raw == null || raw.orderedInstructionIndices.isEmpty()) {
            return null;
        }
        return buildActualFromRaw(raw);
    }

    private static RawBuildResult buildRawGraph(String classBytecodeName, MethodNode method) {
        InsnList insns = method.instructions;
        if (insns == null || insns.size() == 0) {
            return null;
        }

        Map<AbstractInsnNode, Integer> indexOf = new IdentityHashMap<>();
        List<AbstractInsnNode> nodes = new ArrayList<>();
        int idx = 0;
        for (AbstractInsnNode in = insns.getFirst(); in != null; in = in.getNext()) {
            indexOf.put(in, idx++);
            nodes.add(in);
        }

        RawControlFlowGraph raw = new RawControlFlowGraph(classBytecodeName, method.name, method.desc);
        List<Integer> orderedInstructionIndices = new ArrayList<>();
        Map<Integer, Integer> instructionIndexToLineNumber = new LinkedHashMap<>();
        Map<Integer, Integer> instructionIndexToOpcode = new LinkedHashMap<>();
        Set<Integer> branchInstructionIndices = new LinkedHashSet<>();

        Integer currentLine = null;
        for (AbstractInsnNode node : nodes) {
            if (node instanceof LineNumberNode) {
                currentLine = ((LineNumberNode) node).line;
                continue;
            }
            int opcode = node.getOpcode();
            if (opcode == -1) {
                continue;
            }
            int instructionIndex = indexOf.get(node);
            raw.addInstruction(instructionIndex, opcode, currentLine, node);
            orderedInstructionIndices.add(instructionIndex);
            instructionIndexToOpcode.put(instructionIndex, opcode);
            if (currentLine != null) {
                instructionIndexToLineNumber.put(instructionIndex, currentLine);
            }
            if (node instanceof JumpInsnNode) {
                if (opcode != Opcodes.GOTO
                        && opcode != Opcodes.JSR) {
                    branchInstructionIndices.add(instructionIndex);
                }
            }
        }

        if (orderedInstructionIndices.isEmpty()) {
            return null;
        }

        Map<Integer, Integer> instructionIndexToOrder = new HashMap<>();
        for (int pos = 0; pos < orderedInstructionIndices.size(); pos++) {
            instructionIndexToOrder.put(orderedInstructionIndices.get(pos), pos);
        }

        Map<LabelNode, Integer> labelToInstructionIndex = new IdentityHashMap<>();
        for (AbstractInsnNode node : nodes) {
            if (node instanceof LabelNode) {
                int labelIdx = indexOf.get(node);
                Integer next = findFirstInstructionIndexAtOrAfter(labelIdx, orderedInstructionIndices);
                if (next != null) {
                    labelToInstructionIndex.put((LabelNode) node, next);
                }
            }
        }

        for (Integer instructionIndex : orderedInstructionIndices) {
            RawControlFlowGraph.InstructionInfo info = raw.getInstructionInfo(instructionIndex);
            AbstractInsnNode node = info.getNode();
            int opcode = info.getOpcode();
            if (node instanceof JumpInsnNode) {
                JumpInsnNode j = (JumpInsnNode) node;
                Integer targetIdx = labelToInstructionIndex.get(j.label);
                if (targetIdx != null) {
                    raw.addEdge(instructionIndex, targetIdx, false);
                }
                if (opcode != Opcodes.GOTO
                        && opcode != Opcodes.JSR) {
                    Integer fallthrough = nextInstructionIndex(instructionIndex, orderedInstructionIndices, instructionIndexToOrder);
                    if (fallthrough != null) {
                        raw.addEdge(instructionIndex, fallthrough, false);
                    }
                }
            } else if (node instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode sw = (LookupSwitchInsnNode) node;
                Integer dflt = labelToInstructionIndex.get(sw.dflt);
                if (dflt != null) {
                    raw.addEdge(instructionIndex, dflt, false);
                }
                for (Object l : sw.labels) {
                    Integer targetIdx = labelToInstructionIndex.get((LabelNode) l);
                    if (targetIdx != null) {
                        raw.addEdge(instructionIndex, targetIdx, false);
                    }
                }
            } else if (node instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode sw = (TableSwitchInsnNode) node;
                Integer dflt = labelToInstructionIndex.get(sw.dflt);
                if (dflt != null) {
                    raw.addEdge(instructionIndex, dflt, false);
                }
                for (Object l : sw.labels) {
                    Integer targetIdx = labelToInstructionIndex.get((LabelNode) l);
                    if (targetIdx != null) {
                        raw.addEdge(instructionIndex, targetIdx, false);
                    }
                }
            } else {
                if (!isReturnOrThrow(opcode)) {
                    Integer fallthrough = nextInstructionIndex(instructionIndex, orderedInstructionIndices, instructionIndexToOrder);
                    if (fallthrough != null) {
                        raw.addEdge(instructionIndex, fallthrough, false);
                    }
                }
            }
        }

        Set<Integer> handlerInstructionIndices = new LinkedHashSet<>();
        if (method.tryCatchBlocks != null) {
            for (Object o : method.tryCatchBlocks) {
                TryCatchBlockNode tcb = (TryCatchBlockNode) o;
                Integer handlerIdx = labelToInstructionIndex.get(tcb.handler);
                if (handlerIdx == null) {
                    continue;
                }
                handlerInstructionIndices.add(handlerIdx);
                Integer startIdx = indexOf.get(tcb.start);
                Integer endIdx = indexOf.get(tcb.end);
                if (startIdx == null || endIdx == null) {
                    continue;
                }
                for (Integer instructionIndex : orderedInstructionIndices) {
                    if (instructionIndex >= startIdx && instructionIndex < endIdx) {
                        raw.addEdge(instructionIndex, handlerIdx, true);
                    }
                }
            }
        }

        return new RawBuildResult(classBytecodeName,
                method,
                raw,
                orderedInstructionIndices,
                instructionIndexToOrder,
                instructionIndexToLineNumber,
                instructionIndexToOpcode,
                branchInstructionIndices,
                handlerInstructionIndices);
    }

    private static ControlFlowGraph buildActualFromRaw(RawBuildResult raw) {
        List<Integer> ordered = raw.orderedInstructionIndices;
        ControlFlowGraph cfg = new ControlFlowGraph(raw.rawGraph.getClassName(), raw.rawGraph.getMethodName(), raw.rawGraph.getDescriptor());

        Set<Integer> leaders = new LinkedHashSet<>();
        Set<Integer> terminators = new LinkedHashSet<>();
        Integer firstInstruction = ordered.get(0);
        leaders.add(firstInstruction);

        for (Integer instructionIndex : ordered) {
            RawControlFlowGraph.InstructionInfo info = raw.rawGraph.getInstructionInfo(instructionIndex);
            if (info == null) continue;
            AbstractInsnNode node = info.getNode();
            int opcode = info.getOpcode();
            Integer next = nextInstructionIndex(instructionIndex, ordered, raw.instructionIndexToOrder);

            if (node instanceof JumpInsnNode) {
                boolean hasFallthrough = false;
                for (RawControlFlowGraph.Edge edge : raw.rawGraph.getOutgoingEdges(instructionIndex)) {
                    if (edge.isExceptionEdge()) {
                        continue;
                    }
                    int target = edge.getTarget();
                    if (next != null && target == next) {
                        hasFallthrough = true;
                    } else {
                        if (raw.instructionIndexToOrder.containsKey(target)) {
                            leaders.add(target);
                        }
                    }
                }
                if (hasFallthrough && next != null) {
                    leaders.add(next);
                }
                terminators.add(instructionIndex);
            } else if (node instanceof LookupSwitchInsnNode || node instanceof TableSwitchInsnNode) {
                for (RawControlFlowGraph.Edge edge : raw.rawGraph.getOutgoingEdges(instructionIndex)) {
                    if (!edge.isExceptionEdge() && raw.instructionIndexToOrder.containsKey(edge.getTarget())) {
                        leaders.add(edge.getTarget());
                    }
                }
                terminators.add(instructionIndex);
            } else if (isReturnOrThrow(opcode)) {
                terminators.add(instructionIndex);
            } else if (raw.branchInstructionIndices.contains(instructionIndex)) {
                if (next != null) {
                    leaders.add(next);
                }
            }
        }

        for (Integer handler : raw.handlerInstructionIndices) {
            if (handler != null) {
                leaders.add(handler);
            }
        }

        for (Integer terminator : terminators) {
            Integer next = nextInstructionIndex(terminator, ordered, raw.instructionIndexToOrder);
            if (next != null) {
                leaders.add(next);
            }
        }

        leaders.removeIf(idx -> !raw.instructionIndexToOrder.containsKey(idx));
        leaders.add(firstInstruction);

        List<Integer> sortedLeaders = new ArrayList<>(leaders);
        Collections.sort(sortedLeaders);

        List<BasicBlock> blocks = new ArrayList<>();
        Map<Integer, Integer> insnToBlock = new HashMap<>();
        int blockId = 0;
        for (int i = 0; i < sortedLeaders.size(); i++) {
            int startIndex = sortedLeaders.get(i);
            Integer startPos = raw.instructionIndexToOrder.get(startIndex);
            if (startPos == null) {
                continue;
            }
            int endPos;
            if (i + 1 < sortedLeaders.size()) {
                int nextStartIndex = sortedLeaders.get(i + 1);
                endPos = raw.instructionIndexToOrder.get(nextStartIndex) - 1;
            } else {
                endPos = ordered.size() - 1;
            }
            if (endPos < startPos) {
                continue;
            }
            int endIndex = ordered.get(endPos);
            BasicBlock block = new BasicBlock(blockId++, startIndex, endIndex);
            blocks.add(block);
            for (int pos = startPos; pos <= endPos; pos++) {
                insnToBlock.put(ordered.get(pos), block.getId());
            }
        }

        for (BasicBlock block : blocks) {
            cfg.addBlock(block);
        }

        for (BasicBlock block : blocks) {
            int startIdx = block.getStartInstructionIndex();
            int endIdx = block.getEndInstructionIndex();
            // Add normal/control edges only from the last instruction in the block
            for (RawControlFlowGraph.Edge edge : raw.rawGraph.getOutgoingEdges(endIdx)) {
                if (edge.isExceptionEdge()) continue;
                Integer succId = insnToBlock.get(edge.getTarget());
                if (succId != null) {
                    block.addSuccessor(succId);
                }
            }
            // Add exception edges from ANY instruction in the block to the handler
            for (int i = startIdx; i <= endIdx; i++) {
                for (RawControlFlowGraph.Edge edge : raw.rawGraph.getOutgoingEdges(i)) {
                    if (!edge.isExceptionEdge()) continue;
                    Integer succId = insnToBlock.get(edge.getTarget());
                    if (succId != null) {
                        block.addSuccessor(succId);
                    }
                }
            }
        }

        for (BasicBlock block : cfg.getBlocksById().values()) {
            for (Integer succ : block.getSuccessorBlockIds()) {
                BasicBlock succBlock = cfg.getBlocksById().get(succ);
                if (succBlock != null) {
                    succBlock.addPredecessor(block.getId());
                }
            }
        }

        for (Map.Entry<Integer, Integer> e : raw.instructionIndexToLineNumber.entrySet()) {
            cfg.addLineMapping(e.getKey(), e.getValue());
        }
        for (Map.Entry<Integer, Integer> e : raw.instructionIndexToOpcode.entrySet()) {
            cfg.addOpcodeMapping(e.getKey(), e.getValue());
        }
        for (Integer branchIdx : raw.branchInstructionIndices) {
            cfg.addBranchInstructionIndex(branchIdx);
        }

        return cfg;
    }

    private static Integer nextInstructionIndex(int instructionIndex,
                                                List<Integer> orderedInstructionIndices,
                                                Map<Integer, Integer> instructionIndexToOrder) {
        Integer pos = instructionIndexToOrder.get(instructionIndex);
        if (pos == null) {
            return null;
        }
        int nextPos = pos + 1;
        if (nextPos >= orderedInstructionIndices.size()) {
            return null;
        }
        return orderedInstructionIndices.get(nextPos);
    }

    private static Integer findFirstInstructionIndexAtOrAfter(int labelIndex, List<Integer> orderedInstructionIndices) {
        for (Integer idx : orderedInstructionIndices) {
            if (idx >= labelIndex) {
                return idx;
            }
        }
        return null;
    }

    private static final class RawBuildResult {
        final String classBytecodeName;
        final MethodNode method;
        final RawControlFlowGraph rawGraph;
        final List<Integer> orderedInstructionIndices;
        final Map<Integer, Integer> instructionIndexToOrder;
        final Map<Integer, Integer> instructionIndexToLineNumber;
        final Map<Integer, Integer> instructionIndexToOpcode;
        final Set<Integer> branchInstructionIndices;
        final Set<Integer> handlerInstructionIndices;

        RawBuildResult(String classBytecodeName,
                       MethodNode method,
                       RawControlFlowGraph rawGraph,
                       List<Integer> orderedInstructionIndices,
                       Map<Integer, Integer> instructionIndexToOrder,
                       Map<Integer, Integer> instructionIndexToLineNumber,
                       Map<Integer, Integer> instructionIndexToOpcode,
                       Set<Integer> branchInstructionIndices,
                       Set<Integer> handlerInstructionIndices) {
            this.classBytecodeName = classBytecodeName;
            this.method = method;
            this.rawGraph = rawGraph;
            this.orderedInstructionIndices = orderedInstructionIndices;
            this.instructionIndexToOrder = instructionIndexToOrder;
            this.instructionIndexToLineNumber = instructionIndexToLineNumber;
            this.instructionIndexToOpcode = instructionIndexToOpcode;
            this.branchInstructionIndices = branchInstructionIndices;
            this.handlerInstructionIndices = handlerInstructionIndices;
        }
    }

    private static boolean isReturnOrThrow(int opcode) {
        switch (opcode) {
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.RETURN:
            case Opcodes.ATHROW:
                return true;
            default:
                return false;
        }
    }
}


