package org.evomaster.client.java.instrumentation.cfg;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;

class RawControlFlowGraphTest {

    @Test
    void instructionInfoGetIndexReturnsIndexParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        assertEquals(10, raw.getInstructionInfo(10).getIndex());
    }

    @Test
    void instructionInfoGetOpcodeReturnsOpcodeParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        assertEquals(100, raw.getInstructionInfo(10).getOpcode());
    }

    @Test
    void instructionInfoGetLineNumberReturnsLineNumberParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        assertEquals(100, raw.getInstructionInfo(10).getLineNumber());
    }

    @Test
    void instructionInfoGetNodeReturnsNodeParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        assertEquals(null, raw.getInstructionInfo(10).getNode());
    }

    @Test
    void edgeIsExceptionEdgeReturnsFalseIfNotExceptionEdge() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        raw.addInstruction(20, 200, 100, null);
        raw.addEdge(10, 20, false);
        assertFalse(raw.getOutgoingEdges(10).iterator().next().isExceptionEdge());
    }

    @Test
    void edgeIsExceptionEdgeReturnsTrueIfExceptionEdge() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        raw.addInstruction(20, 200, 100, null);
        raw.addEdge(10, 20, true);
        assertTrue(raw.getOutgoingEdges(10).iterator().next().isExceptionEdge());
    }


    @Test
    void edgeGetTargetReturnsTargetParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        raw.addInstruction(20, 200, 100, null);
        raw.addEdge(10, 20, false);
        Collection<RawControlFlowGraph.Edge> edges = raw.getOutgoingEdges(10);
        assertEquals(1, edges.size());
        RawControlFlowGraph.Edge edge = edges.iterator().next();
        assertEquals(20, edge.getTarget());
    }

    @Test
    void edgeIsExceptionEdgeReturnsExceptionEdgeParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        raw.addInstruction(20, 200, 100, null);
        raw.addEdge(10, 20, false);
        Collection<RawControlFlowGraph.Edge> edges = raw.getOutgoingEdges(10);
        assertEquals(1, edges.size());
        RawControlFlowGraph.Edge edge = edges.iterator().next();
        assertFalse(edge.isExceptionEdge());
    }

    @Test
    void edgeEqualsReturnsTrueIfSameTargetAndExceptionEdge() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        raw.addInstruction(20, 200, 100, null);
        raw.addEdge(10, 20, false);

        Collection<RawControlFlowGraph.Edge> edges = raw.getOutgoingEdges(10);
        assertEquals(1, edges.size());
        RawControlFlowGraph.Edge edge = edges.iterator().next();

        assertTrue(edge.equals(new RawControlFlowGraph.Edge(20, false)));
    }

    @Test
    void edgeEqualsReturnsFalseIfDifferentTargetOrExceptionEdge() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        raw.addInstruction(20, 200, 100, null);
        raw.addEdge(10, 20, false);
        Collection<RawControlFlowGraph.Edge> edges = raw.getOutgoingEdges(10);
        assertEquals(1, edges.size());
        RawControlFlowGraph.Edge edge = edges.iterator().next();
        assertFalse(edge.equals(new RawControlFlowGraph.Edge(21, false)));
        assertFalse(edge.equals(new RawControlFlowGraph.Edge(20, true)));
    }

    @Test
    void edgeHashCodeReturnsHashCodeOfTargetAndExceptionEdge() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(10, 100, 100, null);
        raw.addInstruction(20, 200, 100, null);
        raw.addEdge(10, 20, false);
        Collection<RawControlFlowGraph.Edge> edges = raw.getOutgoingEdges(10);
        assertEquals(1, edges.size());
        RawControlFlowGraph.Edge edge = edges.iterator().next();
        assertEquals(Objects.hash(20, false), edge.hashCode());
    }

    @Test
    void rawControlFlowGraphGetClassNameReturnsClassNameParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        assertEquals("C", raw.getClassName());
    }

    @Test
    void rawControlFlowGraphGetMethodNameReturnsMethodNameParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        assertEquals("m", raw.getMethodName());
    }

    @Test
    void rawControlFlowGraphGetDescriptorReturnsDescriptorParameter() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        assertEquals("()V", raw.getDescriptor());
    }

    @Test
    void rawControlFlowGraphAddInstructionAddsInstructionAndOutgoingAndIncomingEdges() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        InsnNode node = new InsnNode(Opcodes.NOP);
        raw.addInstruction(0, 1, 100, node);
        assertTrue(raw.hasInstruction(0));
        RawControlFlowGraph.InstructionInfo info = raw.getInstructionInfo(0);
        assertNotNull(info);
        assertEquals(0, info.getIndex());
        assertEquals(1, info.getOpcode());
        assertEquals(100, info.getLineNumber());
        assertEquals(node, info.getNode());

        assertEquals(0, raw.getOutgoingEdges(0).size());
        assertEquals(0, raw.getIncomingEdges(0).size());
    }

    @Test
    void rawControlFlowGraphGetInstructionIndicesInOrderReturnsInstructionIndicesInOrder() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(0, 1, 100, null);
        raw.addInstruction(1, 2, 100, null);
        raw.addInstruction(2, 3, 101, null);
        List<Integer> indices = raw.getInstructionIndicesInOrder();
        assertEquals(Arrays.asList(0, 1, 2), indices);
    }

    @Test
    void rawControlFlowGraphAddEdgeAddsEdgeAndOutgoingAndIncomingEdges() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(0, 1, 100, null);
        raw.addInstruction(1, 2, 100, null);
        raw.addEdge(0, 1, false);
        assertTrue(raw.hasInstruction(0));
        assertTrue(raw.hasInstruction(1));
        assertEquals(1, raw.getOutgoingEdges(0).size());
        assertEquals(1, raw.getIncomingEdges(1).size());
    }


    @Test
    void rawControlFlowGraphGetOutgoingEdgesReturnsOutgoingEdges() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(0, 1, 100, null);
        raw.addInstruction(1, 2, 100, null);
        raw.addEdge(0, 1, false);
        Collection<RawControlFlowGraph.Edge> edges = raw.getOutgoingEdges(0);
        assertEquals(1, edges.size());
        assertEquals(1, edges.iterator().next().getTarget());
    }

    @Test
    void rawControlFlowGraphGetIncomingEdgesReturnsIncomingEdges() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(0, 1, 100, null);
        raw.addInstruction(1, 2, 100, null);
        raw.addEdge(0, 1, false);
        Collection<RawControlFlowGraph.Edge> edges = raw.getIncomingEdges(1);
        assertEquals(1, edges.size());
        assertEquals(0, edges.iterator().next().getTarget());
    }
}


