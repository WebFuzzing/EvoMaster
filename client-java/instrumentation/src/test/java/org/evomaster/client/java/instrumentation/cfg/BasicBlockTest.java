package org.evomaster.client.java.instrumentation.cfg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicBlockTest {

    @Test
    void createsAndTracksSuccessorsAndPredecessors() {
        BasicBlock b0 = new BasicBlock(0, 0, 3);
        BasicBlock b1 = new BasicBlock(1, 4, 7);
        BasicBlock b2 = new BasicBlock(2, 8, 9);

        b0.addSuccessor(1);
        b1.addPredecessor(0);
        b1.addSuccessor(2);
        b2.addPredecessor(1);

        assertEquals(0, b0.getId());
        assertEquals(0, b0.getStartInstructionIndex());
        assertEquals(3, b0.getEndInstructionIndex());

        assertTrue(b0.getSuccessorBlockIds().contains(1));
        assertTrue(b1.getPredecessorBlockIds().contains(0));
        assertTrue(b1.getSuccessorBlockIds().contains(2));
        assertTrue(b2.getPredecessorBlockIds().contains(1));
    }

    @Test
    void equalityBasedOnIdOnly() {
        BasicBlock a = new BasicBlock(5, 0, 1);
        BasicBlock b = new BasicBlock(5, 10, 11);
        BasicBlock c = new BasicBlock(6, 0, 1);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toStringIncludesIdsRangeAndNeighbors() {
        BasicBlock bb = new BasicBlock(7, 10, 20);
        bb.addSuccessor(8);
        bb.addSuccessor(9);
        bb.addPredecessor(6);

        String expected = "BasicBlock{id=7, start=10, end=20, succ=[8, 9], pred=[6]}";
        assertEquals(expected, bb.toString());
    }
}


