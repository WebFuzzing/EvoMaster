package org.evomaster.client.java.instrumentation.shared;

import org.junit.jupiter.api.Test;

import java.util.Locale;

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

        assertTrue(TaintInputName.isTaintInput("_EM_42_XYZ_"));
    }

    @Test
    public void testInvalidNamePatterns(){
        String prefix = "_EM_";
        String postfix = "_XYZ_";

        assertFalse(TaintInputName.isTaintInput("foo"));
        assertFalse(TaintInputName.isTaintInput(""));
        assertFalse(TaintInputName.isTaintInput(prefix));
        assertFalse(TaintInputName.isTaintInput(prefix + postfix));
        assertFalse(TaintInputName.isTaintInput(prefix+"a"+postfix));

        assertTrue(TaintInputName.isTaintInput(prefix+"42"+postfix));
    }


    @Test
    public void testIncludes(){

        String name = TaintInputName.getTaintName(0);
        String text = "some prefix " + name + " some postfix";

        assertFalse(TaintInputName.isTaintInput(text));
        assertTrue(TaintInputName.includesTaintInput(text));
    }

    @Test
    public void testUpperLowerCase(){

        String name = TaintInputName.getTaintName(0);

        assertTrue(TaintInputName.isTaintInput(name));
        assertTrue(TaintInputName.includesTaintInput(name));


        assertTrue(TaintInputName.isTaintInput(name.toLowerCase()));
        assertTrue(TaintInputName.includesTaintInput(name.toLowerCase()));
        assertTrue(TaintInputName.isTaintInput(name.toUpperCase()));
        assertTrue(TaintInputName.includesTaintInput(name.toUpperCase()));
    }
}