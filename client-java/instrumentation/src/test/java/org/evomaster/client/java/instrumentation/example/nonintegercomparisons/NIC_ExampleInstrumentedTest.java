package org.evomaster.client.java.instrumentation.example.nonintegercomparisons;

import com.foo.somedifferentpackage.examples.branches.BranchesImp;
import com.foo.somedifferentpackage.examples.nonintegercomparisons.NIC_ExampleImp;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.example.branches.Branches;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by arcuri82 on 02-Mar-20.
 */
public class NIC_ExampleInstrumentedTest {

    protected NIC_Example getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (NIC_Example)
                cl.loadClass(NIC_ExampleImp.class.getName()).getDeclaredConstructor().newInstance();
    }

    private int evalPos(long x, long y){

        try {
            return getInstance().pos(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalNeg(long x, long y){

        try {
            return getInstance().neg(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalEq(long x, long y){

        try {
            return getInstance().eq(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private int evalPos(double x, double y){

        try {
            return getInstance().pos(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalNeg(double x, double y){

        try {
            return getInstance().neg(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalEq(double x, double y){

        try {
            return getInstance().eq(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalPos(float x, float y){

        try {
            return getInstance().pos(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalNeg(float x, float y){

        try {
            return getInstance().neg(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalEq(float x, float y){

        try {
            return getInstance().eq(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }




    @BeforeAll
    public static void initClass(){
        ObjectiveRecorder.reset(true);
    }

    @BeforeEach
    public void init(){
        ObjectiveRecorder.reset(false);
        ExecutionTracer.reset();
        assertEquals(0 , ExecutionTracer.getNumberOfObjectives());
    }




    @Test
    public void testPosXLong(){

        testPosX(
                () -> evalPos(10L, 0L),
                () -> evalPos(15L, 0L),
                () -> evalPos(8L, 0L)
        );
    }

    @Test
    public void testPosXDouble(){

        testPosX(
                () -> evalPos(10.42d, 0d),
                () -> evalPos(15.72123d, 0d),
                () -> evalPos(8.0d, 0d)
        );
    }

    @Test
    public void testPosXFloat(){

        testPosX(
                () -> evalPos(10.1f, 0f),
                () -> evalPos(15.42f, 0f),
                () -> evalPos(8f, 0f)
        );
    }

    private void testPosX(Supplier<Integer> firstCall_positiveX,
                         Supplier<Integer> secondCall_worseX,
                         Supplier<Integer> thirdCall_betterX){

        int res = firstCall_positiveX.get();
        //first branch should had been taken
        assertEquals(0, res);

        //so far, seen only first comparison,
        assertEquals(3, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));

        Set<String> nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(2, nonCovered.size());

        for(String id : nonCovered) {
            double h = ExecutionTracer.getValue(id);
            assertTrue(h < 1d); // not covered
            assertTrue(h > 0 ); // it has been reached though
        }

        List<Double> first = extractHeuristicsSorted(nonCovered);


        secondCall_worseX.get(); //worse value, should have no impact
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(2, nonCovered.size());

        List<Double> second = extractHeuristicsSorted(nonCovered);

        for(int i=0; i<first.size(); i++) {
            //no impact, the same
            assertEquals(first.get(i), second.get(i), 0.0001);
        }


        thirdCall_betterX.get(); //better value
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(2, nonCovered.size());

        List<Double> third = extractHeuristicsSorted(nonCovered);

        for(int i=0; i<first.size(); i++) {
            //better
            assertTrue(third.get(i) > second.get(i));
            //but still not covered
            assertTrue(third.get(i) < 1);
        }
    }



    @Test
    public void testPosYLong(){

        testPosY(
                () -> evalPos(10L, 0L),
                () -> evalPos(-2L, 10L),
                () -> evalPos(-2L, 14L),
                () -> evalPos(-2L, 3L),
                () -> evalPos(-8L, -20L)
        );
    }

    @Test
    public void testPosYDouble(){

        testPosY(
                () -> evalPos(10.1d, 0d),
                () -> evalPos(-2.42d, 10.3d),
                () -> evalPos(-2.42d, 14.333d),
                () -> evalPos(-2.42d, 3.1d),
                () -> evalPos(-8d, -20d)
        );
    }

    @Test
    public void testPosYFloat(){

        testPosY(
                () -> evalPos(10.1f, 0f),
                () -> evalPos(-2.42f, 10.3f),
                () -> evalPos(-2.42f, 14.333f),
                () -> evalPos(-2.42f, 3.1f),
                () -> evalPos(-8f, -20f)
        );
    }

    private void testPosY(Supplier<Integer> firstCall_positiveX,
                          Supplier<Integer> secondCall_negativeX,
                          Supplier<Integer> thirdCall_negativeX_but_worseY,
                          Supplier<Integer> fourthCall_negativeX_and_betterY,
                          Supplier<Integer> fifthCall_bothNegative
    ){

        int res = firstCall_positiveX.get();
        //first branch should had been taken
        assertEquals(0, res);


        res = secondCall_negativeX.get();
        //second branch should had been taken
        assertEquals(1, res);


        //so far, 2 comparisons, each one with its own 3 targets
        assertEquals(6, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));

        Set<String> nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        //on CMP for 0 on first IF, and then 2 for second if
        assertEquals(3, nonCovered.size());

        for(String id : nonCovered) {
            double h = ExecutionTracer.getValue(id);
            assertTrue(h < 1d); // not covered
            assertTrue(h > 0 ); // it has been reached though
        }

        List<Double> second = extractHeuristicsSorted(nonCovered);


        thirdCall_negativeX_but_worseY.get(); //worse value, should have no impact
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(3, nonCovered.size());

        List<Double> third = extractHeuristicsSorted(nonCovered);

        for(int i=0; i<second.size(); i++) {
            //no impact, the same
            assertEquals(third.get(i), second.get(i), 0.0001);
        }


        fourthCall_negativeX_and_betterY.get(); //better value
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(3, nonCovered.size());

        List<Double> fourth = extractHeuristicsSorted(nonCovered);

        int better = 0;

        for(int i=0; i<third.size(); i++) {
            //better or equal
            assertTrue(fourth.get(i) >= third.get(i));
            //but still not covered
            assertTrue(fourth.get(i) < 1);

            if(fourth.get(i) > third.get(i)){
                better++;
            }
        }

        //2 objectives (< and =0) should had gotten better
        assertEquals(2, better);


        res = fifthCall_bothNegative.get();
        assertEquals(2, res);
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        //the cases of 0 were not covered
        assertEquals(2, nonCovered.size());
    }

    private List<Double> extractHeuristicsSorted(Set<String> nonCovered) {
        return nonCovered.stream()
                .sorted()
                .map(id -> ExecutionTracer.getValue(id))
                .collect(Collectors.toList());
    }


    @Test
    public void testNegXLong(){

        testNegX(
                () -> evalNeg(-15L, 0L),
                () -> evalNeg(-2215L, 0L),
                () -> evalNeg(-2L, 0L)
        );
    }

    @Test
    public void testNegXDouble(){

        testNegX(
                () -> evalNeg(-15.4d, 0d),
                () -> evalNeg(-2215.16, 0d),
                () -> evalNeg(-2.11111, 0d)
        );
    }

    @Test
    public void testNegXFloat(){

        testNegX(
                () -> evalNeg(-15f, 0f),
                () -> evalNeg(-2215.4444444f, 0f),
                () -> evalNeg(-2.3f, 0f)
        );
    }


    private void testNegX(Supplier<Integer> firstCall_negativeX,
                          Supplier<Integer> secondCall_worseX,
                          Supplier<Integer> thirdCall_betterX){

        int res = firstCall_negativeX.get();
        //first branch should had been taken
        assertEquals(3, res);

        //so far, seen only first comparison,
        assertEquals(3, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));

        Set<String> nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(2, nonCovered.size());

        for(String id : nonCovered) {
            double h = ExecutionTracer.getValue(id);
            assertTrue(h < 1d); // not covered
            assertTrue(h > 0 ); // it has been reached though
        }

        List<Double> first = extractHeuristicsSorted(nonCovered);


        secondCall_worseX.get(); //worse value, should have no impact
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(2, nonCovered.size());

        List<Double> second = extractHeuristicsSorted(nonCovered);

        for(int i=0; i<first.size(); i++) {
            //no impact, the same
            assertEquals(first.get(i), second.get(i), 0.0001);
        }


        thirdCall_betterX.get(); //better value
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(2, nonCovered.size());

        List<Double> third = extractHeuristicsSorted(nonCovered);

        for(int i=0; i<first.size(); i++) {
            //better
            assertTrue(third.get(i) > second.get(i));
            //but still not covered
            assertTrue(third.get(i) < 1);
        }
    }



    @Test
    public void testEqLong(){

        testEq(
                () -> evalEq(0L, 0L),
                () -> evalEq(5L, 7L),
                () -> evalEq(-2L, 0L),
                () -> evalEq(-2L, -4L)
        );
    }

    @Test
    public void testEqDouble(){

        testEq(
                () -> evalEq(0d, 0.0d),
                () -> evalEq(5.222d, 7.1d),
                () -> evalEq(-2.11111d, 0d),
                () -> evalEq(-2d, -4.3d)
        );
    }

    @Test
    public void testEqFloat(){

        testEq(
                () -> evalEq(0.00f, 0f),
                () -> evalEq(5.3f, 7f),
                () -> evalEq(-2f, 0f),
                () -> evalEq(-2.9999f, -4.7777f)
        );
    }


    private void testEq(Supplier<Integer> firstCall_both0,
                          Supplier<Integer> secondCall_bothGreaterThan0,
                          Supplier<Integer> thirdCall_negativeAnd0,
                        Supplier<Integer> fourthCall_bothNegative) {

        int res;
        res = firstCall_both0.get();
        assertEquals(6, res);

        assertEquals(3, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        assertEquals(2, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));


        res = secondCall_bothGreaterThan0.get();
        assertEquals(7, res);

        assertEquals(6, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        assertEquals(3, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));


        res = thirdCall_negativeAnd0.get();
        assertEquals(8, res);

        assertEquals(6, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));

        res = fourthCall_bothNegative.get();
        assertEquals(7, res);

        assertEquals(6, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
    }


    @Test
    public void testAllPos(){

        assertEquals(0, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));

        evalPos(1L,1L);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(0L, 1L);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1L, 1L);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1L, -1L);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1L, 0L);
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        assertEquals(6, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));

        evalPos(1d,1d);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(0d, 1d);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1d, 1d);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1d, -1d);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1d, 0d);
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        assertEquals(12, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));

        evalPos(1f,1f);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(0f, 1f);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1f, 1f);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1f, -1f);
        assertTrue(0 < ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        evalPos(-1f, 0f);
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
        assertEquals(18, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.NUMERIC_COMPARISON));
    }
}
