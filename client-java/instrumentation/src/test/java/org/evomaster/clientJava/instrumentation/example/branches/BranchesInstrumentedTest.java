package org.evomaster.clientJava.instrumentation.example.branches;

import com.foo.somedifferentpackage.examples.branches.BranchesImp;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.ObjectiveNaming;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BranchesInstrumentedTest {


    protected Branches getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (Branches)
                cl.loadClass(BranchesImp.class.getName()).newInstance();
    }

    private int evalPos(int x, int y){

        try {
            return getInstance().pos(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalNeg(int x, int y){

        try {
            return getInstance().neg(x,y);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int evalEq(int x, int y){

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
    public void testPosX(){

        int res = evalPos(10, 0);
        //first branch should had been taken
        assertEquals(0, res);

        //so far, seen only first "if", of which the else is not covered
        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));

        String elseBranch = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH).iterator().next();
        assertTrue(elseBranch.contains(ObjectiveNaming.FALSE_BRANCH));
        assertFalse(elseBranch.contains(ObjectiveNaming.TRUE_BRANCH));

        double first = ExecutionTracer.getValue(elseBranch);
        assertTrue(first < 1d); // not covered


        evalPos(15, 0); //worse value, should have no impact
        double second = ExecutionTracer.getValue(elseBranch);
        assertTrue(second < 1d); // still not covered
        assertEquals(first, second, 0.001);

        evalPos(8, 0); //better value
        double third = ExecutionTracer.getValue(elseBranch);
        assertTrue(third < 1d); // still not covered
        assertTrue(third > first);
    }

    @Test
    public void testPosY() {

        assertEquals(0d, ObjectiveRecorder.computeCoverage(ObjectiveNaming.BRANCH));

        int res;
        res = evalPos(10, 0);
        assertEquals(0, res);

        res = evalPos(-5, 4);
        assertEquals(1, res);

        //seen 2 "if", but returned on the second "if"
        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));

        String elseBranch = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH).iterator().next();
        assertTrue(elseBranch.contains(ObjectiveNaming.FALSE_BRANCH));
        assertFalse(elseBranch.contains(ObjectiveNaming.TRUE_BRANCH));

        double first = ExecutionTracer.getValue(elseBranch);
        assertTrue(first < 1d); // not covered

        evalPos(-8, 8); //worse value, should have no impact
        double second = ExecutionTracer.getValue(elseBranch);
        assertTrue(second < 1d); // still not covered
        assertEquals(first, second, 0.001);

        evalPos(-8, 0); //better value, but still not covered
        double third = ExecutionTracer.getValue(elseBranch);
        assertTrue(third < 1d); // still not covered
        assertTrue(third > second);

        //all branches covered
        res = evalPos(-89, -45);
        assertEquals(2, res);

        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));

        assertTrue(ObjectiveRecorder.computeCoverage(ObjectiveNaming.BRANCH) > 0);
    }


    @Test
    public void testNegX(){

        int res = evalNeg(-10, 0);
        //first branch should had been taken
        assertEquals(3, res);

        //so far, seen only first "if", of which the else is not covered
        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));

        String elseBranch = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH).iterator().next();
        assertTrue(elseBranch.contains(ObjectiveNaming.FALSE_BRANCH));
        assertFalse(elseBranch.contains(ObjectiveNaming.TRUE_BRANCH));

        double first = ExecutionTracer.getValue(elseBranch);
        assertTrue(first < 1d); // not covered


        evalNeg(-15, 0); //worse value, should have no impact
        double second = ExecutionTracer.getValue(elseBranch);
        assertTrue(second < 1d); // still not covered
        assertEquals(first, second, 0.001);

        evalNeg(-8, 0); //better value
        double third = ExecutionTracer.getValue(elseBranch);
        assertTrue(third < 1d); // still not covered
        assertTrue(third > first);
    }


    @Test
    public void testNegY() {

        int res;
        res = evalNeg(-10, 0);
        assertEquals(3, res);

        res = evalNeg(5, -4);
        assertEquals(4, res);

        //seen 2 "if", but returned on the second "if"
        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));

        String elseBranch = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH).iterator().next();
        assertTrue(elseBranch.contains(ObjectiveNaming.FALSE_BRANCH));
        assertFalse(elseBranch.contains(ObjectiveNaming.TRUE_BRANCH));

        double first = ExecutionTracer.getValue(elseBranch);
        assertTrue(first < 1d); // not covered

        evalNeg(8, -8); //worse value, should have no impact
        double second = ExecutionTracer.getValue(elseBranch);
        assertTrue(second < 1d); // still not covered
        assertEquals(first, second, 0.001);

        evalNeg(8, 0); //better value, but still not covered
        double third = ExecutionTracer.getValue(elseBranch);
        assertTrue(third < 1d); // still not covered
        assertTrue(third > second);

        //all branches covered
        res = evalNeg(89, 45);
        assertEquals(5, res);

        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
    }


    @Test
    public void testEq(){

        int res;
        res = evalEq(0, 0);
        assertEquals(6, res);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));

        res = evalEq(2, 5);
        assertEquals(7, res);

        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));

        res = evalEq(2, 0);
        assertEquals(8, res);

        assertEquals(4, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
    }

    @Test
    public void testAll(){

        evalPos(1,1);
        evalPos(-1, 1);
        evalPos(-1, -1);

        evalNeg(-1, -1);
        evalNeg(1, -1);
        evalNeg(1, 1);

        evalEq(0, 0);
        evalEq(4, 0);
        evalEq(5, 5);

        assertEquals(12, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.BRANCH));
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.BRANCH));
    }

}
