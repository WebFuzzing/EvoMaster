package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntryBlockTest {

    private static final String CLASS_NAME = "com.example.EntryBlockFixture";
    private static final String METHOD_NAME = "sample()V";

    @Test
    void entryBlockSetsFlagsAndMetadata() {
        EntryBlock block = new EntryBlock(CLASS_NAME, METHOD_NAME);

        assertTrue(block.isEntryBlock());
        assertFalse(block.isExitBlock());
        assertEquals(CLASS_NAME, block.getClassName());
        assertEquals(METHOD_NAME, block.getMethodName());
    }

    @Test
    void entryBlockProvidesDescriptiveNameAndToString() {
        EntryBlock block = new EntryBlock(CLASS_NAME, METHOD_NAME);

        String expected = "EntryBlock for method " + METHOD_NAME;
        assertEquals(expected, block.getName());
        assertEquals(expected, block.toString());
    }
}

