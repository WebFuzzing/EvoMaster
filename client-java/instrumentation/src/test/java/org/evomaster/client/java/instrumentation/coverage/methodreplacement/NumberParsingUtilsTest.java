package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class NumberParsingUtilsTest {


    @ParameterizedTest
    @ValueSource(strings = {"0","0.",".0","-0","0.0","-0.0","-0.","-.0"})
    public void testOk(String input){
        checkOk(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-",".","-.","0-","1-1","0.0.0"})
    public void testFail(String input){
        checkFail(input);
    }


    private void checkOk(String input){
        Double.parseDouble(input); // no exception
        assertEquals(1d, NumberParsingUtils.getParsingHeuristicValueForFloat(input), 0.0001);
    }

    private void checkFail(String input){

        double distance = NumberParsingUtils.getParsingHeuristicValueForFloat(input);
        assertTrue(distance >= 0.0);
        assertTrue(distance < 1); // not covered
    }
}