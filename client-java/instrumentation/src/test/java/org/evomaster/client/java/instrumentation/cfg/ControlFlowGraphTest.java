package org.evomaster.client.java.instrumentation.cfg;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Arrays;


import static org.junit.jupiter.api.Assertions.*;

class ControlFlowGraphTest {

    @Test
    void getClassNameReturnsClassNameParameter() {
        ControlFlowGraph cfg = new ControlFlowGraph("pkg/MyClass", "compute", "(I)I");
        assertEquals("pkg/MyClass", cfg.getClassName());
    }

    @Test
    void getMethodNameReturnsMethodNameParameter() {
        ControlFlowGraph cfg = new ControlFlowGraph("pkg/MyClass", "compute", "(I)I");
        assertEquals("compute", cfg.getMethodName());
    }

    @Test
    void getDescriptorReturnsDescriptorParameter() {
        ControlFlowGraph cfg = new ControlFlowGraph("pkg/MyClass", "compute", "(I)I");
        assertEquals("(I)I", cfg.getDescriptor());
    }

    @Test
    void addBlockIndexesByIdAndInstructionRangeAndSetsEntryOnFirstAdd() {
        ControlFlowGraph cfg = new ControlFlowGraph("pkg/MyClass", "compute", "(I)I");
        BasicBlock block = new BasicBlock(10, 5, 8);

        cfg.addBlock(block);

        assertSame(block, cfg.getBlocksById().get(10));
        assertEquals(Integer.valueOf(10), cfg.blockIdForInstructionIndex(5));
        assertEquals(Integer.valueOf(10), cfg.blockIdForInstructionIndex(6));
        assertEquals(Integer.valueOf(10), cfg.blockIdForInstructionIndex(7));
        assertEquals(Integer.valueOf(10), cfg.blockIdForInstructionIndex(8));
        assertEquals(Integer.valueOf(10), cfg.getEntryBlockId());
    }

    @Test
    void addBlockDoesNotChangeEntryOnSecondAdd() {
        ControlFlowGraph cfg = new ControlFlowGraph("pkg/MyClass", "compute", "(I)I");
        BasicBlock first = new BasicBlock(1, 0, 2);
        BasicBlock second = new BasicBlock(2, 3, 4);

        cfg.addBlock(first);
        assertEquals(Integer.valueOf(1), cfg.getEntryBlockId());

        cfg.addBlock(second);
        assertEquals(Integer.valueOf(1), cfg.getEntryBlockId());
    }

    @Test
    void blockIdForInstructionIndexReturnsRightBlockIdWhenMultipleBlocksAreAdded() {
        ControlFlowGraph cfg = new ControlFlowGraph("C", "m", "(I)V");
        BasicBlock b0 = new BasicBlock(0, 0, 2);
        BasicBlock b1 = new BasicBlock(1, 3, 5);
        cfg.addBlock(b0);
        cfg.addBlock(b1);

        assertEquals(Integer.valueOf(0), cfg.blockIdForInstructionIndex(0));
        assertEquals(Integer.valueOf(0), cfg.blockIdForInstructionIndex(2));
        assertEquals(Integer.valueOf(1), cfg.blockIdForInstructionIndex(3));
        assertEquals(Integer.valueOf(1), cfg.blockIdForInstructionIndex(5));
    }

    @Test
    void addLineMappingAddsLineMappingAndIndexInstructionIndicesByLine() {
        ControlFlowGraph cfg = new ControlFlowGraph("C", "m", "(I)V");
        cfg.addLineMapping(10, 100);
        cfg.addLineMapping(11, 100);
        cfg.addLineMapping(12, 101);
        cfg.addLineMapping(13, 102);
        cfg.addLineMapping(14, 102);

        // Check that (instruction --> line number) is correct
        Map<Integer, Integer> insnToLine = cfg.getInstructionIndexToLineNumber();
        assertEquals(Integer.valueOf(100), insnToLine.get(10));
        assertEquals(Integer.valueOf(100), insnToLine.get(11));
        assertEquals(Integer.valueOf(101), insnToLine.get(12));
        assertEquals(Integer.valueOf(102), insnToLine.get(13));
        assertEquals(Integer.valueOf(102), insnToLine.get(14));

        // Check that (line number --> instruction indices) is correct
        assertEquals(Arrays.asList(10, 11), cfg.getInstructionIndicesForLine(100));
        assertEquals(Arrays.asList(12), cfg.getInstructionIndicesForLine(101));
        assertEquals(Arrays.asList(13, 14), cfg.getInstructionIndicesForLine(102));
    }

    @Test
    void addOpcodeMappingAddsInstructionIndexToOpcodeMapping() {
        ControlFlowGraph cfg = new ControlFlowGraph("C", "m", "(I)V");
        cfg.addOpcodeMapping(10, 100);
        cfg.addOpcodeMapping(11, 100);
        cfg.addOpcodeMapping(12, 101);
        cfg.addOpcodeMapping(13, 102);
        cfg.addOpcodeMapping(14, 102);

        // Check that (instruction index --> opcode) is correct
        Map<Integer, Integer> insnToOpcode = cfg.getInstructionIndexToOpcode();
        assertEquals(Integer.valueOf(100), insnToOpcode.get(10));
        assertEquals(Integer.valueOf(100), insnToOpcode.get(11));
        assertEquals(Integer.valueOf(101), insnToOpcode.get(12));
        assertEquals(Integer.valueOf(102), insnToOpcode.get(13));
        assertEquals(Integer.valueOf(102), insnToOpcode.get(14));
    }


    @Test
    void addBranchInstructionIndexAddsBranchInstructionIndexAndIndexInstructionIndicesByLine() {
        ControlFlowGraph cfg = new ControlFlowGraph("C", "m", "(I)V");

        // Add line mappings
        cfg.addLineMapping(10, 100);
        cfg.addLineMapping(11, 100);
        cfg.addLineMapping(12, 101);
        cfg.addLineMapping(13, 102);
        cfg.addLineMapping(14, 102);

        // Add branch instruction indices
        cfg.addBranchInstructionIndex(10);
        cfg.addBranchInstructionIndex(11);
        cfg.addBranchInstructionIndex(12);
        cfg.addBranchInstructionIndex(13);
        cfg.addBranchInstructionIndex(14);

        // Check that branchInstructionIndices contains the correct instruction indices
        assertTrue(cfg.getBranchInstructionIndices().contains(10));
        assertTrue(cfg.getBranchInstructionIndices().contains(11));
        assertTrue(cfg.getBranchInstructionIndices().contains(12));
        assertTrue(cfg.getBranchInstructionIndices().contains(13));
        assertTrue(cfg.getBranchInstructionIndices().contains(14));

        // Check that (line number --> branch instruction indices) is correct
        assertEquals(Arrays.asList(10, 11), cfg.getBranchInstructionIndicesForLine(100));
        assertEquals(Arrays.asList(12), cfg.getBranchInstructionIndicesForLine(101));
        assertEquals(Arrays.asList(13, 14), cfg.getBranchInstructionIndicesForLine(102));
    }
}


