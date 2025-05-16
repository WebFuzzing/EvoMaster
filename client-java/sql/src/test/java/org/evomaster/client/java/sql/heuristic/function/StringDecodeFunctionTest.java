package org.evomaster.client.java.sql.heuristic.function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringDecodeFunctionTest {

    @Test
    public void testDecodeHelloWorld() {
        StringDecodeFunction stringDecodeFunction = new StringDecodeFunction();
        String input = "Hello, World!";
        String expectedOutput = "Hello, World!";
        String actualOutput = (String) stringDecodeFunction.evaluate(input);
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void testDecodeUnicodeString() {
        StringDecodeFunction stringDecodeFunction = new StringDecodeFunction();
        String input = "\\uffff";
        String expectedOutput = String.valueOf('\uffff');
        String actualOutput = (String) stringDecodeFunction.evaluate(input);
        assertEquals(expectedOutput, actualOutput);
    }

}
