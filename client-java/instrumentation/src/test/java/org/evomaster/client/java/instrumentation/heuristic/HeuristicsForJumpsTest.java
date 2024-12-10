package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.evomaster.client.java.instrumentation.heuristic.HeuristicsForJumps.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * The calculation of branch distances is a tricky business, so it
 * is paramount that it is fully tested in details.
 * If something goes wrong, it would be very difficult to find
 * out with system tests.
 */
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


    @Test
    public void test_IF_ICMPEQ_pos(){
        // x == y
        int code = Opcodes.IF_ICMPEQ;

        Truthness t = getForValueComparison(5, 5, code);
        assertTrue(t.isTrue());
        assertFalse(t.isFalse());
    }

    @Test
    public void test_IF_ICMPEQ_neg(){
        // x == y
        int code = Opcodes.IF_ICMPEQ;

        Truthness t = getForValueComparison(-8, -8, code);
        assertTrue(t.isTrue());
        assertFalse(t.isFalse());
    }

    @Test
    public void test_IF_ICMPEQ_0(){
        // x == y
        int code = Opcodes.IF_ICMPEQ;

        Truthness t = getForValueComparison(0, 0, code);
        assertTrue(t.isTrue());
        assertFalse(t.isFalse());
    }

    @Test
    public void test_IF_ICMPEQ_dif(){
        // x == y
        int code = Opcodes.IF_ICMPEQ;

        Truthness t = getForValueComparison(2, 4, code);
        assertFalse(t.isTrue());
        assertTrue(t.isFalse());
    }

    @Test
    public void test_IF_ICMPEQ_dif_incr(){
        // x == y
        int code = Opcodes.IF_ICMPEQ;

        Truthness a = getForValueComparison(3, 5, code);
        assertTrue(a.isFalse());

        Truthness b = getForValueComparison(7, 8, code);
        assertTrue(b.isFalse());

        assertTrue(b.getOfTrue() > a.getOfTrue());
    }

    @Test
    public void test_IF_ICMPEQ_dif_spec(){
        // x == y
        int code = Opcodes.IF_ICMPEQ;

        Truthness a = getForValueComparison(-3, 5, code);
        assertTrue(a.isFalse());

        Truthness b = getForValueComparison(-1, 6, code);
        assertTrue(b.isFalse());

        assertTrue(b.getOfTrue() > a.getOfTrue());
    }

    @Test
    public void test_IF_ICMPEQ_dif_negPos(){
        // x == y
        int code = Opcodes.IF_ICMPEQ;

        Truthness a = getForValueComparison(-3, -50, code);
        assertTrue(a.isFalse());

        Truthness b = getForValueComparison(10, 6, code);
        assertTrue(b.isFalse());

        assertTrue(b.getOfTrue() > a.getOfTrue());
    }


    @Test
    public void test_IF_ICMPNE_eq(){
        //val != 0

        int[] values = new int[]{-10, -2, 0, 3, 4444};
        for(int val: values){

            Truthness ne = getForValueComparison(val, val, Opcodes.IF_ICMPNE);
            Truthness eq = getForValueComparison(val, val, Opcodes.IF_ICMPEQ);

            //their values should be inverted
            assertEquals(ne.getOfTrue(), eq.getOfFalse(), 0.001);
            assertEquals(ne.getOfFalse(), eq.getOfTrue(), 0.001);
        }
    }

    @Test
    public void test_IF_ICMPNE_diff(){
        //val != 0

        int x = 1;

        int[] values = new int[]{-10, -2, 0, 3, 4444};
        for(int val: values){

            Truthness ne = getForValueComparison(val, x, Opcodes.IF_ICMPNE);
            Truthness eq = getForValueComparison(val, x, Opcodes.IF_ICMPEQ);

            //their values should be inverted
            assertEquals(ne.getOfTrue(), eq.getOfFalse(), 0.001);
            assertEquals(ne.getOfFalse(), eq.getOfTrue(), 0.001);
        }
    }

    @Test
    public void test_IF_ICMPLT_true(){
        // x < y
        int code = Opcodes.IF_ICMPLT;

        Truthness t = getForValueComparison(4, 6, code);
        assertTrue(t.isTrue());
        assertFalse(t.isFalse());
    }

    @Test
    public void test_IF_ICMPLT_false(){
        // x < y
        int code = Opcodes.IF_ICMPLT;

        Truthness t = getForValueComparison(6, 4, code);
        assertFalse(t.isTrue());
        assertTrue(t.isFalse());
    }

    @Test
    public void test_IF_ICMPLT_pos_true(){
        // x < y
        int code = Opcodes.IF_ICMPLT;

        Truthness a = getForValueComparison(4, 6, code);
        assertFalse(a.isFalse());

        Truthness b = getForValueComparison(1, 5, code);
        assertFalse(b.isFalse());

        assertTrue(a.getOfFalse() > b.getOfFalse());
    }

    @Test
    public void test_IF_ICMPLT_neg_true(){
        // x < y
        int code = Opcodes.IF_ICMPLT;

        Truthness a = getForValueComparison(-8, -6, code);
        assertFalse(a.isFalse());

        Truthness b = getForValueComparison(-100, -5, code);
        assertFalse(b.isFalse());

        assertTrue(a.getOfFalse() > b.getOfFalse());
    }


    @Test
    public void test_IF_ICMPLT_pos_false(){
        // x < y
        int code = Opcodes.IF_ICMPLT;

        Truthness a = getForValueComparison(10, 6, code);
        assertFalse(a.isTrue());

        Truthness b = getForValueComparison(222, 5, code);
        assertFalse(b.isTrue());

        assertTrue(a.getOfTrue() > b.getOfTrue());
    }

    @Test
    public void test_IF_ICMPLT_neg_false(){
        // x < y
        int code = Opcodes.IF_ICMPLT;

        Truthness a = getForValueComparison(-6, -10, code);
        assertFalse(a.isTrue());

        Truthness b = getForValueComparison(-5, -222, code);
        assertFalse(b.isTrue());

        assertTrue(a.getOfTrue() > b.getOfTrue());
    }

    @Test
    public void test_IF_ICMPLT_0() {
        // x < y
        int code = Opcodes.IF_ICMPLT;

        int[] values = new int[]{-5, -2, 0, 3, 7};
        for(int val : values){

            Truthness lt = getForSingleValueJump(val, Opcodes.IFLT);
            Truthness cmp = getForValueComparison(val, 0, Opcodes.IF_ICMPLT);

            //should be the same
            assertEquals(lt.getOfTrue(), cmp.getOfTrue(), 0.001);
            assertEquals(lt.getOfFalse(), cmp.getOfFalse(), 0.001);
        }
    }

    @Test
    public void test_IF_ICMPGE() {
        // x >= y
        int y = 1;

        int[] values = new int[]{-5, -2, 0, 3, 7};
        for(int val : values){

            Truthness lt = getForValueComparison(val, y, Opcodes.IF_ICMPLT);
            Truthness ge = getForValueComparison(val, y, Opcodes.IF_ICMPGE);

            //should be the inverted
            assertEquals(lt.getOfTrue(), ge.getOfFalse(), 0.001);
            assertEquals(lt.getOfFalse(), ge.getOfTrue(), 0.001);
        }
    }

    @Test
    public void test_IF_ICMPLE() {
        // x <= y  implies  ! (y < x)
        int y = 1;

        int[] values = new int[]{-5, -2, 0, 3, 7};
        for(int val : values){

            Truthness le = getForValueComparison(val, y, Opcodes.IF_ICMPLE);
            Truthness lt = getForValueComparison(y, val, Opcodes.IF_ICMPLT).invert();

            //should be the same
            assertEquals(lt.getOfTrue(), le.getOfTrue(), 0.001);
            assertEquals(lt.getOfFalse(), le.getOfFalse(), 0.001);
        }
    }

    @Test
    public void test_IF_ICMPGT() {
        // x > y  implies  y < x
        int y = 1;

        int[] values = new int[]{-5, -2, 0, 3, 7};
        for(int val : values){

            Truthness gt = getForValueComparison(val, y, Opcodes.IF_ICMPGT);
            Truthness lt = getForValueComparison(y, val, Opcodes.IF_ICMPLT);

            //should be the same
            assertEquals(lt.getOfTrue(), gt.getOfTrue(), 0.001);
            assertEquals(lt.getOfFalse(), gt.getOfFalse(), 0.001);
        }
    }

    @Test
    public void test_IF_ACMPEQ_true(){
        // x == y
        int code = Opcodes.IF_ACMPEQ;

        Truthness t = getForObjectComparison("a", "a", code);
        assertTrue(t.isTrue());
        assertFalse(t.isFalse());
    }

    @Test
    public void test_IF_ACMPEQ_false(){
        // x == y
        int code = Opcodes.IF_ACMPEQ;

        Object a = new Object();
        Integer b = 5;

        Truthness t = getForObjectComparison(a, b, code);
        assertFalse(t.isTrue());
        assertTrue(t.isFalse());
    }

    @Test
    public void test_IF_ACMPNE(){
        // x != y

        Object x = "a";
        Object[] values = new Object[]{new Object(), x, "foo", 5};
        for(Object val: values){

            Truthness eq = getForObjectComparison(x, val, Opcodes.IF_ACMPEQ);
            Truthness ne = getForObjectComparison(x, val, Opcodes.IF_ACMPNE);

            //should be inverted
            assertEquals(eq.getOfTrue(), ne.getOfFalse(), 0.001);
            assertEquals(eq.getOfFalse(), ne.getOfTrue(), 0.001);
        }
    }


    @Test
    public void test_IFNULL(){
        // x == null

        Object[] values = new Object[]{null, new Object(), "foo", 5};
        for(Object val: values){

            Truthness eq = getForObjectComparison(val, null, Opcodes.IF_ACMPEQ);
            Truthness nu = getForNullComparison(val, Opcodes.IFNULL);

            //should be equivalent
            assertEquals(eq.getOfTrue(), nu.getOfTrue(), 0.001);
            assertEquals(eq.getOfFalse(), nu.getOfFalse(), 0.001);
        }
    }

    @Test
    public void test_IFNONNULL(){
        // x != null

        Object[] values = new Object[]{null, new Object(), "foo", 5};
        for(Object val: values){

            Truthness nn = getForNullComparison(val, Opcodes.IFNONNULL);
            Truthness nu = getForNullComparison(val, Opcodes.IFNULL);

            //should be inverted
            assertEquals(nn.getOfFalse(), nu.getOfTrue(), 0.001);
            assertEquals(nn.getOfTrue(), nu.getOfFalse(), 0.001);
        }
    }

}