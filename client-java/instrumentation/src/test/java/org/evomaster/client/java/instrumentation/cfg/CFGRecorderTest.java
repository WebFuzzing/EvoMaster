package org.evomaster.client.java.instrumentation.cfg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CFGRecorderTest {

    @Test
    void registerGetAndReset() {
        CFGRecorder.reset();
        ControlFlowGraph cfg = new ControlFlowGraph("A", "foo", "(I)I");
        cfg.addBlock(new BasicBlock(0, 0, 1));
        CFGRecorder.register(cfg);

        ControlFlowGraph found = CFGRecorder.get("A", "foo", "(I)I");
        assertNotNull(found);
        assertEquals("A", found.getClassName());
        assertEquals("foo", found.getMethodName());
        assertEquals("(I)I", found.getDescriptor());

        CFGRecorder.reset();
        assertNull(CFGRecorder.get("A", "foo", "(I)I"));
    }
}


