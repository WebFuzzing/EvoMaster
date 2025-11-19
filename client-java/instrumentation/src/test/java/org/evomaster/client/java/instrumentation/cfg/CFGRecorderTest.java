package org.evomaster.client.java.instrumentation.cfg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CFGRecorderTest {

    @Test
    void getReturnsRegisteredGraph() {
        CFGRecorder.reset();
        ControlFlowGraph cfg = new ControlFlowGraph("A", "foo", "(I)I");
        cfg.addBlock(new BasicBlock(0, 0, 1));
        CFGRecorder.register(cfg);

        ControlFlowGraph found = CFGRecorder.get("A", "foo", "(I)I");
        assertNotNull(found);
        assertEquals("A", found.getClassName());
        assertEquals("foo", found.getMethodName());
        assertEquals("(I)I", found.getDescriptor());
    }

    @Test
    void getReturnsNullAfterReset() {
        CFGRecorder.reset();
        ControlFlowGraph cfg = new ControlFlowGraph("A", "foo", "(I)I");
        cfg.addBlock(new BasicBlock(0, 0, 1));
        CFGRecorder.register(cfg);
        CFGRecorder.reset();
        assertNull(CFGRecorder.get("A", "foo", "(I)I"));
    }

    @Test
    void getAllReturnsAllRegisteredGraphs() {
        CFGRecorder.reset();

        ControlFlowGraph cfg1 = new ControlFlowGraph("A", "foo", "(I)I");
        cfg1.addBlock(new BasicBlock(0, 0, 1));
        CFGRecorder.register(cfg1);

        ControlFlowGraph cfg2 = new ControlFlowGraph("B", "bar", "()V");
        cfg2.addBlock(new BasicBlock(0, 0, 1));
        CFGRecorder.register(cfg2);

        List<ControlFlowGraph> all = CFGRecorder.getAll();
        assertEquals(2, all.size());

        boolean hasFirst = false;
        boolean hasSecond = false;
        for (ControlFlowGraph g : all) {
            if ("A".equals(g.getClassName()) && "foo".equals(g.getMethodName()) && "(I)I".equals(g.getDescriptor())) {
                hasFirst = true;
            }
            if ("B".equals(g.getClassName()) && "bar".equals(g.getMethodName()) && "()V".equals(g.getDescriptor())) {
                hasSecond = true;
            }
        }
        assertTrue(hasFirst);
        assertTrue(hasSecond);
    }
}


