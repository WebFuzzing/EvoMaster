package org.evomaster.client.java.instrumentation.cfg;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ControlFlowGraphTest {

    @Test
    void mapsBlocksAndInstructionIndices() {
        ControlFlowGraph cfg = new ControlFlowGraph("C", "m", "(I)V");
        BasicBlock b0 = new BasicBlock(0, 0, 2);
        BasicBlock b1 = new BasicBlock(1, 3, 5);
        cfg.addBlock(b0);
        cfg.addBlock(b1);

        assertEquals(0, cfg.getEntryBlockId());
        assertEquals(Integer.valueOf(0), cfg.blockIdForInstructionIndex(0));
        assertEquals(Integer.valueOf(0), cfg.blockIdForInstructionIndex(2));
        assertEquals(Integer.valueOf(1), cfg.blockIdForInstructionIndex(3));
        assertEquals(Integer.valueOf(1), cfg.blockIdForInstructionIndex(5));
    }

    @Test
    void lineAndBranchIndicesPerLine() {
        ControlFlowGraph cfg = new ControlFlowGraph("C", "m", "()V");

        // Create a single block covering instructions 10..14
        BasicBlock b0 = new BasicBlock(0, 10, 14);
        cfg.addBlock(b0);

        // Line mappings
        cfg.addLineMapping(10, 100);
        cfg.addLineMapping(11, 100);
        cfg.addLineMapping(12, 101);
        cfg.addLineMapping(13, 102);
        cfg.addLineMapping(14, 102);

        // Opcodes (values arbitrary for test)
        cfg.addOpcodeMapping(10, 1);
        cfg.addOpcodeMapping(11, 2);
        cfg.addOpcodeMapping(12, 3);

        // Branch index on a line
        cfg.addBranchInstructionIndex(11);
        cfg.addBranchInstructionIndex(13);

        Map<Integer, Integer> insnToLine = cfg.getInstructionIndexToLineNumber();
        assertEquals(Integer.valueOf(100), insnToLine.get(10));
        assertEquals(Integer.valueOf(100), insnToLine.get(11));
        assertEquals(Integer.valueOf(101), insnToLine.get(12));

        assertEquals(2, cfg.getInstructionIndicesForLine(100).size());
        assertEquals(2, cfg.getBranchInstructionIndicesForLine(100).size() + cfg.getBranchInstructionIndicesForLine(102).size());

        assertTrue(cfg.getBranchInstructionIndices().contains(11));
        assertTrue(cfg.getBranchInstructionIndices().contains(13));
    }
}


