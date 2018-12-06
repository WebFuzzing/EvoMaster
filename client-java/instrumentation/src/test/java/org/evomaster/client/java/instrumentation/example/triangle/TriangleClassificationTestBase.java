package org.evomaster.client.java.instrumentation.example.triangle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class TriangleClassificationTestBase {

    /*
        Note: it is important that the interface TriangleClassification
        is under the org.evomaster package, so it does not get
        instrumented.
        If it was, then we would end up with casting problems here, as
        the same class/interface loaded by two different classloaders
        would not match
     */

    protected abstract TriangleClassification getInstance() throws Exception;

    private TriangleClassification.Classification eval(int a, int b, int c){
        try {
            return getInstance().classify(a,b,c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNegative(){
        Assertions.assertEquals(TriangleClassification.Classification.NOT_A_TRIANGLE, eval(-1, 1, 1));
    }

    @Test
    public void testAllZeros(){
        Assertions.assertEquals(TriangleClassification.Classification.NOT_A_TRIANGLE, eval(0, 0, 0));
    }

    @Test
    public void testEquilateral(){
        Assertions.assertEquals(TriangleClassification.Classification.EQUILATERAL, eval(1, 1, 1));
    }

    @Test
    public void testIsosceles(){
        Assertions.assertEquals(TriangleClassification.Classification.ISOSCELES, eval(3, 2, 2));
    }

    @Test
    public void testScalene(){
        Assertions.assertEquals(TriangleClassification.Classification.SCALENE, eval(4, 3, 2));
    }

    @Test
    public void testTooLong(){
        Assertions.assertEquals(TriangleClassification.Classification.NOT_A_TRIANGLE, eval(200, 3, 2));
    }

    @Test
    public void testEquilateralLarge(){
        Assertions.assertEquals(TriangleClassification.Classification.EQUILATERAL,
                eval(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testIsoscelesLarge(){
        Assertions.assertEquals(TriangleClassification.Classification.ISOSCELES,
                eval(Integer.MAX_VALUE-1, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void testScaleneLarge(){
        Assertions.assertEquals(TriangleClassification.Classification.SCALENE,
                eval(Integer.MAX_VALUE-1, Integer.MAX_VALUE-2, Integer.MAX_VALUE));
    }

}
