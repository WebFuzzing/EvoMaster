package org.evomaster.client.java.instrumentation.cfg;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.*;

/**
 * Instruction-level CFG.
 * Nodes correspond to individual bytecode instructions (opcode != -1). Edges can be marked as exception edges.
 */
public final class RawControlFlowGraph {

    public static final class InstructionInfo {
        private final int index;
        private final int opcode;
        private final Integer lineNumber;
        private final AbstractInsnNode node;

        InstructionInfo(int index, int opcode, Integer lineNumber, AbstractInsnNode node) {
            this.index = index;
            this.opcode = opcode;
            this.lineNumber = lineNumber;
            this.node = node;
        }

        public int getIndex() {
            return index;
        }

        public int getOpcode() {
            return opcode;
        }

        public Integer getLineNumber() {
            return lineNumber;
        }

        public AbstractInsnNode getNode() {
            return node;
        }
    }

    public static final class Edge {
        private final int target;
        private final boolean exceptionEdge;

        Edge(int target, boolean exceptionEdge) {
            this.target = target;
            this.exceptionEdge = exceptionEdge;
        }

        public int getTarget() {
            return target;
        }

        public boolean isExceptionEdge() {
            return exceptionEdge;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return target == edge.target && exceptionEdge == edge.exceptionEdge;
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, exceptionEdge);
        }
    }

    private final String className;
    private final String methodName;
    private final String descriptor;

    private final LinkedHashMap<Integer, InstructionInfo> instructions = new LinkedHashMap<>();
    private final Map<Integer, LinkedHashSet<Edge>> outgoingEdges = new LinkedHashMap<>();
    private final Map<Integer, LinkedHashSet<Edge>> incomingEdges = new LinkedHashMap<>();

    public RawControlFlowGraph(String className, String methodName, String descriptor) {
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

    public void addInstruction(int index, int opcode, Integer lineNumber, AbstractInsnNode node) {
        InstructionInfo info = new InstructionInfo(index, opcode, lineNumber, node);
        instructions.put(index, info);
        outgoingEdges.computeIfAbsent(index, k -> new LinkedHashSet<>());
        incomingEdges.computeIfAbsent(index, k -> new LinkedHashSet<>());
    }

    public boolean hasInstruction(int index) {
        return instructions.containsKey(index);
    }

    public InstructionInfo getInstructionInfo(int index) {
        return instructions.get(index);
    }

    public List<Integer> getInstructionIndicesInOrder() {
        return new ArrayList<>(instructions.keySet());
    }

    public void addEdge(int from, int to, boolean exceptionEdge) {
        if (!instructions.containsKey(from) || !instructions.containsKey(to)) {
            return;
        }
        Edge edge = new Edge(to, exceptionEdge);
        outgoingEdges.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(edge);
        incomingEdges.computeIfAbsent(to, k -> new LinkedHashSet<>()).add(new Edge(from, exceptionEdge));
    }

    public Collection<Edge> getOutgoingEdges(int from) {
        LinkedHashSet<Edge> edges = outgoingEdges.get(from);
        if (edges == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(edges);
    }

    public Collection<Edge> getIncomingEdges(int to) {
        LinkedHashSet<Edge> edges = incomingEdges.get(to);
        if (edges == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(edges);
    }
}


