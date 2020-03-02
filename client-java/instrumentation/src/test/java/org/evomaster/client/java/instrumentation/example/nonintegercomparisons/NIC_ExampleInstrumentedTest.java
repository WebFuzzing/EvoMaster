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

    private void testPosX(Supplier<Integer> firstCall,
                         Supplier<Integer> secondCall,
                         Supplier<Integer> thirdCall){

        int res = firstCall.get();
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

        List<Double> first = nonCovered.stream()
                .sorted()
                .map(id -> ExecutionTracer.getValue(id))
                .collect(Collectors.toList());


        secondCall.get(); //worse value, should have no impact
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(2, nonCovered.size());

        List<Double> second = nonCovered.stream()
                .sorted()
                .map(id -> ExecutionTracer.getValue(id))
                .collect(Collectors.toList());

        for(int i=0; i<first.size(); i++) {
            //no impact, the same
            assertEquals(first.get(i), second.get(i), 0.0001);
        }


        thirdCall.get(); //better value
        nonCovered = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.NUMERIC_COMPARISON);
        assertEquals(2, nonCovered.size());

        List<Double> third = nonCovered.stream()
                .sorted()
                .map(id -> ExecutionTracer.getValue(id))
                .collect(Collectors.toList());

        for(int i=0; i<first.size(); i++) {
            //better
            assertTrue(third.get(i) > second.get(i));
            //but still not covered
            assertTrue(third.get(i) < 1);
        }

    }



//    @Test
//    public void testPosY() {
//
//        assertEquals(0d, ObjectiveRecorder.computeCoverage(ObjectiveNaming.BRANCH));
//
//        int res;
//        res = evalPos(10, 0);
//        assertEquals(0, res);
//
//        res = evalPos(-5, 4);
//        assertEquals(1, res);
//
//        //seen 2 "if", but returned on the second "if"
//        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//
//        String elseBranch = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH).iterator().next();
//        assertTrue(elseBranch.contains(ObjectiveNaming.FALSE_BRANCH));
//        assertFalse(elseBranch.contains(ObjectiveNaming.TRUE_BRANCH));
//
//        double first = ExecutionTracer.getValue(elseBranch);
//        assertTrue(first < 1d); // not covered
//
//        evalPos(-8, 8); //worse value, should have no impact
//        double second = ExecutionTracer.getValue(elseBranch);
//        assertTrue(second < 1d); // still not covered
//        assertEquals(first, second, 0.001);
//
//        evalPos(-8, 0); //better value, but still not covered
//        double third = ExecutionTracer.getValue(elseBranch);
//        assertTrue(third < 1d); // still not covered
//        assertTrue(third > second);
//
//        //all branches covered
//        res = evalPos(-89, -45);
//        assertEquals(2, res);
//
//        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//
//        assertTrue(ObjectiveRecorder.computeCoverage(ObjectiveNaming.BRANCH) > 0);
//    }
//
//
//    @Test
//    public void testNegX(){
//
//        int res = evalNeg(-10, 0);
//        //first branch should had been taken
//        assertEquals(3, res);
//
//        //so far, seen only first "if", of which the else is not covered
//        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//
//        String elseBranch = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH).iterator().next();
//        assertTrue(elseBranch.contains(ObjectiveNaming.FALSE_BRANCH));
//        assertFalse(elseBranch.contains(ObjectiveNaming.TRUE_BRANCH));
//
//        double first = ExecutionTracer.getValue(elseBranch);
//        assertTrue(first < 1d); // not covered
//
//
//        evalNeg(-15, 0); //worse value, should have no impact
//        double second = ExecutionTracer.getValue(elseBranch);
//        assertTrue(second < 1d); // still not covered
//        assertEquals(first, second, 0.001);
//
//        evalNeg(-8, 0); //better value
//        double third = ExecutionTracer.getValue(elseBranch);
//        assertTrue(third < 1d); // still not covered
//        assertTrue(third > first);
//    }
//
//
//    @Test
//    public void testNegY() {
//
//        int res;
//        res = evalNeg(-10, 0);
//        assertEquals(3, res);
//
//        res = evalNeg(5, -4);
//        assertEquals(4, res);
//
//        //seen 2 "if", but returned on the second "if"
//        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//
//        String elseBranch = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH).iterator().next();
//        assertTrue(elseBranch.contains(ObjectiveNaming.FALSE_BRANCH));
//        assertFalse(elseBranch.contains(ObjectiveNaming.TRUE_BRANCH));
//
//        double first = ExecutionTracer.getValue(elseBranch);
//        assertTrue(first < 1d); // not covered
//
//        evalNeg(8, -8); //worse value, should have no impact
//        double second = ExecutionTracer.getValue(elseBranch);
//        assertTrue(second < 1d); // still not covered
//        assertEquals(first, second, 0.001);
//
//        evalNeg(8, 0); //better value, but still not covered
//        double third = ExecutionTracer.getValue(elseBranch);
//        assertTrue(third < 1d); // still not covered
//        assertTrue(third > second);
//
//        //all branches covered
//        res = evalNeg(89, 45);
//        assertEquals(5, res);
//
//        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//    }
//
//
//    @Test
//    public void testEq(){
//
//        int res;
//        res = evalEq(0, 0);
//        assertEquals(6, res);
//
//        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//
//        res = evalEq(2, 5);
//        assertEquals(7, res);
//
//        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//
//        res = evalEq(2, 0);
//        assertEquals(8, res);
//
//        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//    }
//
//    @Test
//    public void testAll(){
//
//        evalPos(1,1);
//        evalPos(-1, 1);
//        evalPos(-1, -1);
//
//        evalNeg(-1, -1);
//        evalNeg(1, -1);
//        evalNeg(1, 1);
//
//        evalEq(0, 0);
//        evalEq(4, 0);
//        evalEq(5, 5);
//
//        assertEquals(12, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
//        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
//    }
}
