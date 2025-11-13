package org.evomaster.client.java.instrumentation.cfg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RawControlFlowGraphTest {

    @Test
    void addInstructionsAndEdges() {
        RawControlFlowGraph raw = new RawControlFlowGraph("C", "m", "()V");
        raw.addInstruction(0, 1, 100, null);
        raw.addInstruction(1, 2, 100, null);
        raw.addInstruction(2, 3, 101, null);

        // normal edge 0->1, exception edge 1->2
        raw.addEdge(0, 1, false);
        raw.addEdge(1, 2, true);

        assertTrue(raw.hasInstruction(0));
        assertTrue(raw.hasInstruction(1));
        assertTrue(raw.hasInstruction(2));

        assertEquals(1, raw.getOutgoingEdges(0).size());
        assertEquals(1, raw.getIncomingEdges(1).size());
        assertEquals(1, raw.getOutgoingEdges(1).size());
        assertEquals(1, raw.getIncomingEdges(2).size());

        RawControlFlowGraph.InstructionInfo info1 = raw.getInstructionInfo(1);
        assertNotNull(info1);
        assertEquals(2, info1.getOpcode());
        assertEquals(Integer.valueOf(100), info1.getLineNumber());
    }
}


