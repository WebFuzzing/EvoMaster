package org.evomaster.client.java.instrumentation.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TaintInputNameTest {


    @Test
    public void testBase(){

        String name = TaintInputName.getTaintName(0);
        assertTrue(TaintInputName.isTaintInput(name));
    }

    @Test
    public void testNegativeId(){
        assertThrows(Exception.class, () -> TaintInputName.getTaintName(-1));
    }

    @Test
    public void testInvalidNames(){
        assertFalse(TaintInputName.isTaintInput("foo"));
        assertFalse(TaintInputName.isTaintInput(""));
        assertFalse(TaintInputName.isTaintInput("evomaster"));
        assertFalse(TaintInputName.isTaintInput("evomaster_input"));
        assertFalse(TaintInputName.isTaintInput("evomaster__input"));
        assertFalse(TaintInputName.isTaintInput("evomaster_a_input"));

        assertTrue(TaintInputName.isTaintInput("evomaster_42_input"));
    }

    @Test
    public void testIncludes(){

        String name = TaintInputName.getTaintName(0);
        String text = "some prefix " + name + " some postfix";

        assertFalse(TaintInputName.isTaintInput(text));
        assertTrue(TaintInputName.includesTaintInput(text));
    }
}