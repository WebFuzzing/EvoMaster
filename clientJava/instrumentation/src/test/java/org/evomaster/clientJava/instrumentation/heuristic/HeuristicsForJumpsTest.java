package org.evomaster.clientJava.instrumentation.heuristic;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.evomaster.clientJava.instrumentation.heuristic.HeuristicsForJumps.getForSingleValueJump;
import static org.junit.jupiter.api.Assertions.*;
import static org.evomaster.clientJava.instrumentation.heuristic.HeuristicsForJumps.*;

public class HeuristicsForJumpsTest {


    @Test
    public void test_IFEQ_0() {
        //val == 0
        int code = Opcodes.IFEQ;

        Truthness t0 = getForSingleValueJump(0, code);
        assertTrue(t0.isTrue());
        assertFalse(t0.isFalse());
    }

    @Test
    public void test_IFEQ_posNeg() {
        //val == 0
        int code = Opcodes.IFEQ;
        int val = +1;

        Truthness tneg = getForSingleValueJump(-val, code);
        assertFalse(tneg.isTrue());
        assertTrue(tneg.isFalse());

        Truthness tpos = getForSingleValueJump(+val, code);
        assertFalse(tpos.isTrue());
        assertTrue(tpos.isFalse());

        // +1 and -1 should lead to same branch distance
        assertTrue(tneg.getOfTrue() < 1d);
        assertEquals(tneg.getOfTrue(), tpos.getOfTrue(), 0.001);
    }


    @Test
    public void test_IFEQ_incr() {
        //val == 0
        int code = Opcodes.IFEQ;

        Truthness a = getForSingleValueJump(1, code);
        Truthness b = getForSingleValueJump(-10, code);

        assertTrue(a.isFalse());
        assertTrue(b.isFalse());

        // 1 is closer to 0
        assertTrue(a.getOfTrue() > b.getOfTrue());
    }

    @Test
    public void test_IFNE(){
        //val != 0

        int[] values = new int[]{-10, -2, 0, 3, 4444};
        for(int val: values){

            Truthness ne = getForSingleValueJump(val, Opcodes.IFNE);
            Truthness eq = getForSingleValueJump(val, Opcodes.IFEQ);

            //their values should be inverted
            assertEquals(ne.getOfTrue(), eq.getOfFalse(), 0.001);
            assertEquals(ne.getOfFalse(), eq.getOfTrue(), 0.001);
        }
    }

    @Test
    public void test_IFLT_m10(){
        //val < 0
        int code = Opcodes.IFLT;

        Truthness t0 = getForSingleValueJump(-10, code);
        assertTrue(t0.isTrue());
        assertFalse(t0.isFalse());
    }

    @Test
    public void test_IFLT_pos(){
        //val < 0
        int code = Opcodes.IFLT;

        Truthness t3 = getForSingleValueJump(3, code);
        assertFalse(t3.isTrue());
        assertTrue(t3.isFalse());

        Truthness t5 = getForSingleValueJump(5, code);
        assertFalse(t5.isTrue());
        assertTrue(t5.isFalse());

        Truthness t12 = getForSingleValueJump(12, code);
        assertFalse(t12.isTrue());
        assertTrue(t12.isFalse());

        assertTrue(t5.getOfTrue() < t3.getOfTrue());
        assertTrue(t5.getOfTrue() > t12.getOfTrue());
    }

    @Test
    public void test_IFLT_neg(){
        //val < 0
        int code = Opcodes.IFLT;

        Truthness tm3 = getForSingleValueJump(-3, code);
        assertTrue(tm3.isTrue());
        assertFalse(tm3.isFalse());

        Truthness tm5 = getForSingleValueJump(-5, code);
        assertTrue(tm5.isTrue());
        assertFalse(tm5.isFalse());

        Truthness tm12 = getForSingleValueJump(-12, code);
        assertTrue(tm12.isTrue());
        assertFalse(tm12.isFalse());

        assertTrue(tm5.getOfFalse() < tm3.getOfFalse());
        assertTrue(tm5.getOfFalse() > tm12.getOfFalse());
    }

    @Test
    public void test_IFLT_0() {
        //val < 0
        int code = Opcodes.IFLT;

        Truthness tm3 = getForSingleValueJump(0, code);
        assertFalse(tm3.isTrue());
        assertTrue(tm3.isFalse());
    }

    @Test
    public void test_IFGE(){
        //val >= 0

        int[] values = new int[]{-11, -3, 0, 5, 7};
        for(int val: values){

            Truthness lt = getForSingleValueJump(val, Opcodes.IFLT);
            Truthness ge = getForSingleValueJump(val, Opcodes.IFGE);

            //their values should be inverted
            assertEquals(lt.getOfTrue(), ge.getOfFalse(), 0.001);
            assertEquals(lt.getOfFalse(), ge.getOfTrue(), 0.001);
        }
    }

    @Test
    public void test_IFLE(){
        //val <= 0  implies !(-val < 0)

        int[] values = new int[]{-2345, -6, 0, 2, 7888};
        for(int val: values){

            Truthness le = getForSingleValueJump(val, Opcodes.IFLE);
            Truthness x = getForSingleValueJump(-val, Opcodes.IFLT).invert();

            //their values should be the same, as equivalent
            assertEquals(le.getOfTrue(), x.getOfTrue(), 0.001);
            assertEquals(le.getOfFalse(), x.getOfFalse(), 0.001);
        }
    }


    @Test
    public void test_IFGT(){
        //val > 0

        int[] values = new int[]{-2345, -63, 0, 211, 7888};
        for(int val: values){

            Truthness gt = getForSingleValueJump(val, Opcodes.IFGT);
            Truthness le = getForSingleValueJump(val, Opcodes.IFLE);

            //their values should be inverted
            assertEquals(gt.getOfTrue(), le.getOfFalse(), 0.001);
            assertEquals(gt.getOfFalse(), le.getOfTrue(), 0.001);
        }
    }
}