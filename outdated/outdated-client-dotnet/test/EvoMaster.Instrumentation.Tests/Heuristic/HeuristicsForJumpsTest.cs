using EvoMaster.Instrumentation.Heuristic;
using Mono.Cecil.Cil;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Heuristic{
    public class HeuristicsForJumpsTest{
        [Fact]
        public void testBrfalse(){
            //val == 0
            Code code = Code.Brfalse;

            Truthness t0 = HeuristicsForJumps.GetForSingleValueJump(0, code);
            Assert.True(t0.IsTrue());
            Assert.False(t0.IsFalse());
        }

        [Fact]
        public void test_Brfalse_posNeg(){
            //val == 0
            Code code = Code.Brfalse;
            int val = +1;

            Truthness tneg = HeuristicsForJumps.GetForSingleValueJump(-val, code);
            Assert.False(tneg.IsTrue());
            Assert.True(tneg.IsFalse());

            Truthness tpos = HeuristicsForJumps.GetForSingleValueJump(+val, code);
            Assert.False(tpos.IsTrue());
            Assert.True(tpos.IsFalse());

            // +1 and -1 should lead to same branch distance
            Assert.True(tneg.GetOfTrue() < 1d);
            //assertEqual(tneg.GetOfTrue(), tpos.GetOfTrue(), 0.001);
            Assert.Equal(tneg.GetOfTrue(), tpos.GetOfTrue());
        }

        [Fact]
        public void test_Brfalse_incr(){
            //val == 0
            Code code = Code.Brfalse;

            Truthness a = HeuristicsForJumps.GetForSingleValueJump(1, code);
            Truthness b = HeuristicsForJumps.GetForSingleValueJump(-10, code);

            Assert.True(a.IsFalse());
            Assert.True(b.IsFalse());

            // 1 is closer to 0
            Assert.True(a.GetOfTrue() > b.GetOfTrue());
        }

        [Theory]
        [InlineData(-10)]
        [InlineData(-2)]
        [InlineData(0)]
        [InlineData(3)]
        [InlineData(4444)]
        public void test_Brtrue(int val){
            //val != 0

            Truthness ne = HeuristicsForJumps.GetForSingleValueJump(val, Code.Brfalse);
            Truthness eq = HeuristicsForJumps.GetForSingleValueJump(val, Code.Brtrue);

            //their values should be inverted
            Assert.Equal(ne.GetOfTrue(), eq.GetOfFalse());
            Assert.Equal(ne.GetOfFalse(), eq.GetOfTrue());
        }

        /**
         * there is no specific opcode for less than 0
         * then use GetForValueComparison(val, 0) here
         */
        [Fact]
        public void test_Blt_m10(){
            //val < 0
            Code code = Code.Blt;

            Truthness t0 = HeuristicsForJumps.GetForValueComparison( -10, 0, code);
            Assert.True(t0.IsTrue());
            Assert.False(t0.IsFalse());
        }

        /**
         * there is no specific opcode for less than 0
         * then use GetForValueComparison(val, 0) here
         */
        [Fact]
        public void test_Blt_pos(){
            //val < 0
            Code code = Code.Blt;

            Truthness t3 = HeuristicsForJumps.GetForValueComparison(3, 0, code);
            Assert.False(t3.IsTrue());
            Assert.True(t3.IsFalse());

            Truthness t5 = HeuristicsForJumps.GetForValueComparison(5, 0, code);
            Assert.False(t5.IsTrue());
            Assert.True(t5.IsFalse());

            Truthness t12 = HeuristicsForJumps.GetForValueComparison(12, 0, code);
            Assert.False(t12.IsTrue());
            Assert.True(t12.IsFalse());

            Assert.True(t5.GetOfTrue() < t3.GetOfTrue());
            Assert.True(t5.GetOfTrue() > t12.GetOfTrue());
        }

        /**
         * there is no specific opcode for less than 0
         * then use GetForValueComparison(val, 0) here
         */
        [Fact]
        public void test_Blt_neg(){
            //val < 0
            Code code = Code.Blt;

            Truthness tm3 = HeuristicsForJumps.GetForValueComparison(-3, 0,code);
            Assert.True(tm3.IsTrue());
            Assert.False(tm3.IsFalse());

            Truthness tm5 = HeuristicsForJumps.GetForValueComparison(-5, 0,code);
            Assert.True(tm5.IsTrue());
            Assert.False(tm5.IsFalse());

            Truthness tm12 = HeuristicsForJumps.GetForValueComparison(-12, 0, code);
            Assert.True(tm12.IsTrue());
            Assert.False(tm12.IsFalse());

            Assert.True(tm5.GetOfFalse() < tm3.GetOfFalse());
            Assert.True(tm5.GetOfFalse() > tm12.GetOfFalse());
        }

        /**
         * there is no specific opcode for less than 0
         * then use GetForValueComparison(val, 0) here
         */
        [Fact]
        public void test_Blt_0(){
            //val < 0
            Code code = Code.Blt;

            Truthness tm3 = HeuristicsForJumps.GetForValueComparison(0, 0, code);
            Assert.False(tm3.IsTrue());
            Assert.True(tm3.IsFalse());
        }

        /**
         * there is no specific opcode for greater than 0
         * then use GetForValueComparison(val, 0) here
         */
        [Theory]
        [InlineData(-11)]
        [InlineData(-3)]
        [InlineData(0)]
        [InlineData(5)]
        [InlineData(7)]
        public void test_Bge_0(int val){
            //val >= 0

            Truthness lt = HeuristicsForJumps.GetForValueComparison(val, 0, Code.Blt);
            Truthness ge = HeuristicsForJumps.GetForValueComparison(val, 0, Code.Bge);

            //their values should be inverted
            Assert.Equal(lt.GetOfTrue(), ge.GetOfFalse());
            Assert.Equal(lt.GetOfFalse(), ge.GetOfTrue());
        }
        /**
         * there is no specific opcode for less than or or equal to 0
         * then use GetForValueComparison(val, 0) here
         */
        [Theory]
        [InlineData(-2345)]
        [InlineData(-63)]
        [InlineData(211)]
        [InlineData(7888)]
        public void test_Ble(int val){
            //val <= 0  implies !(-val < 0)

            Truthness le = HeuristicsForJumps.GetForValueComparison(val, 0, Code.Ble);
            Truthness x = HeuristicsForJumps.GetForValueComparison(-val, 0, Code.Blt).Invert();

            //their values should be the same, as equivalent
            Assert.Equal(le.GetOfTrue(), x.GetOfTrue());
            Assert.Equal(le.GetOfFalse(), x.GetOfFalse());
        }


        /**
         * there is no specific opcode for greater than or or equal to 0
         * then use GetForValueComparison(val, 0) here
         */
        [Theory]
        [InlineData(-2345)]
        [InlineData(-63)]
        [InlineData(211)]
        [InlineData(7888)]
        public void test_Bge(int val){
            //val > 0

            Truthness gt = HeuristicsForJumps.GetForValueComparison(val, 0, Code.Bgt);
            Truthness le = HeuristicsForJumps.GetForValueComparison(val, 0, Code.Ble);

            //their values should be inverted
            Assert.Equal(gt.GetOfTrue(), le.GetOfFalse());
            Assert.Equal(gt.GetOfFalse(), le.GetOfTrue());
        }


        [Fact]
        public void test_Beq_pos(){
            // x == y
            Code code = Code.Beq;

            Truthness t = HeuristicsForJumps.GetForValueComparison(5, 5, code);
            Assert.True(t.IsTrue());
            Assert.False(t.IsFalse());
        }

        [Fact]
        public void test_Beq_neg(){
            // x == y
            Code code = Code.Beq;

            Truthness t = HeuristicsForJumps.GetForValueComparison(-8, -8, code);
            Assert.True(t.IsTrue());
            Assert.False(t.IsFalse());
        }

        [Fact]
        public void test_Beq_0(){
            // x == y
            Code code = Code.Beq;

            Truthness t = HeuristicsForJumps.GetForValueComparison(0, 0, code);
            Assert.True(t.IsTrue());
            Assert.False(t.IsFalse());
        }

        [Fact]
        public void test_Beq_dif(){
            // x == y
            Code code = Code.Beq;

            Truthness t = HeuristicsForJumps.GetForValueComparison(2, 4, code);
            Assert.False(t.IsTrue());
            Assert.True(t.IsFalse());
        }

        [Fact]
        public void test_Beq_dif_incr(){
            // x == y
            Code code = Code.Beq;

            Truthness a = HeuristicsForJumps.GetForValueComparison(3, 5, code);
            Assert.True(a.IsFalse());

            Truthness b = HeuristicsForJumps.GetForValueComparison(7, 8, code);
            Assert.True(b.IsFalse());

            Assert.True(b.GetOfTrue() > a.GetOfTrue());
        }

        [Fact]
        public void test_Beq_dif_spec(){
            // x == y
            Code code = Code.Beq;

            Truthness a = HeuristicsForJumps.GetForValueComparison(-3, 5, code);
            Assert.True(a.IsFalse());

            Truthness b = HeuristicsForJumps.GetForValueComparison(-1, 6, code);
            Assert.True(b.IsFalse());

            Assert.True(b.GetOfTrue() > a.GetOfTrue());
        }

        [Fact]
        public void test_Beq_dif_negPos(){
            // x == y
            Code code = Code.Beq;

            Truthness a = HeuristicsForJumps.GetForValueComparison(-3, -50, code);
            Assert.True(a.IsFalse());

            Truthness b = HeuristicsForJumps.GetForValueComparison(10, 6, code);
            Assert.True(b.IsFalse());

            Assert.True(b.GetOfTrue() > a.GetOfTrue());
        }


        [Theory]
        [InlineData(-10)]
        [InlineData(-2)]
        [InlineData(0)]
        [InlineData(3)]
        [InlineData(4444)]
        public void test_Beq_eq(int val){
            //val != 0

            Truthness ne = HeuristicsForJumps.GetForValueComparison(val, val, Code.Bne_Un);
            Truthness eq = HeuristicsForJumps.GetForValueComparison(val, val, Code.Beq);

            //their values should be inverted
            Assert.Equal(ne.GetOfTrue(), eq.GetOfFalse());
            Assert.Equal(ne.GetOfFalse(), eq.GetOfTrue());
        }

        [Theory]
        [InlineData(-10)]
        [InlineData(-2)]
        [InlineData(0)]
        [InlineData(3)]
        [InlineData(4444)]
        public void test_Beq_diff(int val){
            //val != 0

            int x = 1;

            Truthness ne = HeuristicsForJumps.GetForValueComparison(val, x, Code.Bne_Un);
            Truthness eq = HeuristicsForJumps.GetForValueComparison(val, x, Code.Beq);

            //their values should be inverted
            Assert.Equal(ne.GetOfTrue(), eq.GetOfFalse());
            Assert.Equal(ne.GetOfFalse(), eq.GetOfTrue());
        }

        [Fact]
        public void test_Blt_two_true(){
            // x < y
            Code code = Code.Blt;

            Truthness t = HeuristicsForJumps.GetForValueComparison(4, 6, code);
            Assert.True(t.IsTrue());
            Assert.False(t.IsFalse());
        }

        [Fact]
        public void test_Blt_two_false(){
            // x < y
            Code code = Code.Blt;

            Truthness t = HeuristicsForJumps.GetForValueComparison(6, 4, code);
            Assert.False(t.IsTrue());
            Assert.True(t.IsFalse());
        }

        [Fact]
        public void test_Blt_two_pos_true(){
            // x < y
            Code code = Code.Blt;

            Truthness a = HeuristicsForJumps.GetForValueComparison(4, 6, code);
            Assert.False(a.IsFalse());

            Truthness b = HeuristicsForJumps.GetForValueComparison(1, 5, code);
            Assert.False(b.IsFalse());

            Assert.True(a.GetOfFalse() > b.GetOfFalse());
        }

        [Fact]
        public void test_Blt_two_neg_true(){
            // x < y
            Code code = Code.Blt;

            Truthness a = HeuristicsForJumps.GetForValueComparison(-8, -6, code);
            Assert.False(a.IsFalse());

            Truthness b = HeuristicsForJumps.GetForValueComparison(-100, -5, code);
            Assert.False(b.IsFalse());

            Assert.True(a.GetOfFalse() > b.GetOfFalse());
        }


        [Fact]
        public void test_Blt_two_pos_false(){
            // x < y
            Code code = Code.Blt;

            Truthness a = HeuristicsForJumps.GetForValueComparison(10, 6, code);
            Assert.False(a.IsTrue());

            Truthness b = HeuristicsForJumps.GetForValueComparison(222, 5, code);
            Assert.False(b.IsTrue());

            Assert.True(a.GetOfTrue() > b.GetOfTrue());
        }

        [Fact]
        public void test_Blt_two_neg_false(){
            // x < y
            Code code = Code.Blt;

            Truthness a = HeuristicsForJumps.GetForValueComparison(-6, -10, code);
            Assert.False(a.IsTrue());

            Truthness b = HeuristicsForJumps.GetForValueComparison(-5, -222, code);
            Assert.False(b.IsTrue());

            Assert.True(a.GetOfTrue() > b.GetOfTrue());
        }

        /*
         Since there is no specific opcode for less than  0
         then we do not need such test for check if they are same
         */
        // [Fact]
        // public void test_Blt_two_0(){
        //     // x < y
        //     Code code = Code.Blt;
        //
        //     int[] values = new int[]{-5, -2, 0, 3, 7};
        //     foreach (int val  in values){
        //         Truthness lt = HeuristicsForJumps.GetForValueComparison(val, 0, Code.Blt);
        //         Truthness cmp = HeuristicsForJumps.GetForValueComparison(val, 0, Code.Blt);
        //
        //         //should be the same
        //         Assert.Equal(lt.GetOfTrue(), cmp.GetOfTrue());
        //         Assert.Equal(lt.GetOfFalse(), cmp.GetOfFalse());
        //     }
        // }

        [Theory]
        [InlineData(-5)]
        [InlineData(-2)]
        [InlineData(0)]
        [InlineData(3)]
        [InlineData(7)]
        public void test_Bge_two(int val){
            // x >= y
            int y = 1;

            Truthness lt = HeuristicsForJumps.GetForValueComparison(val, y, Code.Blt);
            Truthness ge = HeuristicsForJumps.GetForValueComparison(val, y, Code.Bge);

            //should be the inverted
            Assert.Equal(lt.GetOfTrue(), ge.GetOfFalse());
            Assert.Equal(lt.GetOfFalse(), ge.GetOfTrue());
        }

        [Theory]
        [InlineData(-5)]
        [InlineData(-2)]
        [InlineData(0)]
        [InlineData(3)]
        [InlineData(7)]
        public void test_Ble_two(int val){
            // x <= y  implies  ! (y < x)
            int y = 1;

            Truthness le = HeuristicsForJumps.GetForValueComparison(val, y, Code.Ble);
            Truthness lt = HeuristicsForJumps.GetForValueComparison(y, val,Code.Blt).Invert();

            //should be the same
            Assert.Equal(lt.GetOfTrue(), le.GetOfTrue());
            Assert.Equal(lt.GetOfFalse(), le.GetOfFalse());
        }

        [Theory]
        [InlineData(-5)]
        [InlineData(-2)]
        [InlineData(0)]
        [InlineData(3)]
        [InlineData(7)]
        public void test_Bgt_two(int val){
            // x > y  implies  y < x
            int y = 1;

            Truthness gt = HeuristicsForJumps.GetForValueComparison(val, y, Code.Bgt);
            Truthness lt = HeuristicsForJumps.GetForValueComparison(y, val, Code.Blt);

            //should be the same
            Assert.Equal(lt.GetOfTrue(), gt.GetOfTrue());
            Assert.Equal(lt.GetOfFalse(), gt.GetOfFalse());
        }

        /*
         Man: there is no specific branch for object comparision
         then do not need following tests for dotnet
         */
        // [Fact]
        // public void test_IF_ACMPEQ_true(){
        //     // x == y
        //     Code code = Code.IF_ACMPEQ;
        //
        //     Truthness t = getForObjectComparison("a", "a", code);
        //     Assert.True(t.IsTrue());
        //     Assert.False(t.IsFalse());
        // }

        // [Fact]
        // public void test_IF_ACMPEQ_false(){
        //     // x == y
        //     Code code = Code.IF_ACMPEQ;
        //
        //     Object a = new Object();
        //     Integer b = 5;
        //
        //     Truthness t = getForObjectComparison(a, b, code);
        //     Assert.False(t.IsTrue());
        //     Assert.True(t.IsFalse());
        // }

        // [Fact]
        // public void test_IF_ACMPNE(){
        //     // x != y
        //
        //     Object x = "a";
        //     Object[] values = new Object[]{new Object(), x, "foo", 5};
        //     for (var val in values){
        //         Truthness eq = getForObjectComparison(x, val, Opcodes.IF_ACMPEQ);
        //         Truthness ne = getForObjectComparison(x, val, Opcodes.IF_ACMPNE);
        //
        //         //should be inverted
        //         Assert.Equal(eq.GetOfTrue(), ne.GetOfFalse(), 0.001);
        //         Assert.Equal(eq.GetOfFalse(), ne.GetOfTrue(), 0.001);
        //     }
        // }


        // [Fact]
        // public void test_IFNULL(){
        //     // x == null
        //
        //     Object[] values = new Object[]{null, new Object(), "foo", 5};
        //     for (Object val:
        //     values){
        //         Truthness eq = getForObjectComparison(val, null, Opcodes.IF_ACMPEQ);
        //         Truthness nu = getForNullComparison(val, Opcodes.IFNULL);
        //
        //         //should be equivalent
        //         Assert.Equal(eq.GetOfTrue(), nu.GetOfTrue(), 0.001);
        //         Assert.Equal(eq.GetOfFalse(), nu.GetOfFalse(), 0.001);
        //     }
        // }

        // [Fact]
        // public void test_IFNONNULL(){
        //     // x != null
        //
        //     Object[] values = new Object[]{null, new Object(), "foo", 5};
        //     for (Object val:
        //     values){
        //         Truthness nn = getForNullComparison(val, Opcodes.IFNONNULL);
        //         Truthness nu = getForNullComparison(val, Opcodes.IFNULL);
        //
        //         //should be inverted
        //         Assert.Equal(nn.GetOfFalse(), nu.GetOfTrue(), 0.001);
        //         Assert.Equal(nn.GetOfTrue(), nu.GetOfFalse(), 0.001);
        //     }
        // }
    }
}