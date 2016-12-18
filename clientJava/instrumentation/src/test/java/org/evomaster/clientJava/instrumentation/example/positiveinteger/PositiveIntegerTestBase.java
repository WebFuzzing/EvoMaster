package org.evomaster.clientJava.instrumentation.example.positiveinteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class PositiveIntegerTestBase {

    protected abstract PositiveInteger getInstance() throws Exception;

    private boolean eval(int x){

        try {
            return getInstance().isPositive(x);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testIsPositiveTrue(){

        boolean b = eval(1000);
        assertTrue(b);
    }


    @Test
    public void testIsPositiveFalse(){

        boolean b = eval(-42);
        assertFalse(b);
    }

    @Test
    public void testIsPositiveLargeValue(){

        boolean b = eval(2_000_000_000);
        assertTrue(b);
    }



}
