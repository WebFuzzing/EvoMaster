package org.evomaster.clientJava.instrumentation.example.branches;

import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.example.positiveinteger.PositiveInteger;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;
import org.foo.somedifferentpackage.examples.branches.BranchesImp;
import org.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchesInstrumentedTest {


    protected Branches getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("org.foo");

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


    @BeforeEach
    public void init(){
        ObjectiveRecorder.reset();
        ExecutionTracer.reset();
        assertEquals(0 , ExecutionTracer.getNumberOfObjectives());
    }


    @Test
    public void testPos(){

        int res = evalPos(10, 0);
        //first branch should had been taken
        assertEquals(0, res);

        //so far, seen only first "if", of which the else is not covered
        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ExecutionTracer.BRANCH));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ExecutionTracer.BRANCH));

        String elseBranch = ExecutionTracer.getNonCoveredObjectives(ExecutionTracer.BRANCH).iterator().next();
        assertTrue(elseBranch.contains(ExecutionTracer.FALSE_BRANCH));
        assertFalse(elseBranch.contains(ExecutionTracer.TRUE_BRANCH));

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

    //TODO other cases
}
