package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExitBlockTest {

    private static final String CLASS_NAME = "com.example.ExitBlockFixture";
    private static final String METHOD_NAME = "sample()V";

    @Test
    void exitBlockSetsFlagsAndMetadata() {
        ExitBlock block = new ExitBlock(CLASS_NAME, METHOD_NAME);

        assertTrue(block.isExitBlock());
        assertFalse(block.isEntryBlock());
        assertEquals(CLASS_NAME, block.getClassName());
        assertEquals(METHOD_NAME, block.getMethodName());
    }

    @Test
    void exitBlockProvidesDescriptiveNameAndToString() {
        ExitBlock block = new ExitBlock(CLASS_NAME, METHOD_NAME);

        String expected = "ExitBlock for method " + METHOD_NAME;
        assertEquals(expected, block.getName());
        assertEquals(expected, block.toString());
    }
}

