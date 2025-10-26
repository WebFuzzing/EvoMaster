using EvoMaster.Instrumentation.Heuristic;
using Mono.Cecil.Cil;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Heuristic{
    public class HeuristicsForNonintegerCompaisonTest{
        
        [Fact]
        public void testCeqFloat(){
            Code code = Code.Ceq;

            Truthness t0 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(0.42f, 0.42f, code);
            Assert.True(t0.IsTrue());
            Assert.False(t0.IsFalse());    
        }
        
        [Theory]
        [InlineData(0.42d, 0.42d)]
        [InlineData(1.0/0.0, 1.0/0.0)]
        [InlineData(-1.0/0.0, -1.0/0.0)]
        public void testCeqDouble(double a, double b){
            Code code = Code.Ceq;

            Truthness t0 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(a, b, code);
            Assert.True(t0.IsTrue());
            Assert.False(t0.IsFalse());    
        }
        
        [Fact]
        public void testCeqLong(){
            Code code = Code.Ceq;

            Truthness t0 = HeuristicsForNonintegerComparisons.GetForLongComparison(42L, 42L, code);
            Assert.True(t0.IsTrue());
            Assert.False(t0.IsFalse());    
        }
        
        
        [Fact]
        public void testCltFloat(){
            Code code = Code.Clt;
            
            Truthness t3 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(0.3f, 0f, code);
            Assert.False(t3.IsTrue());
            Assert.True(t3.IsFalse());

            Truthness t5 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(0.5f, 0f, code);
            Assert.False(t5.IsTrue());
            Assert.True(t5.IsFalse());

            Truthness t12 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(1.2f, 0f, code);
            Assert.False(t12.IsTrue());
            Assert.True(t12.IsFalse());

            Assert.True(t5.GetOfTrue() < t3.GetOfTrue());
            Assert.True(t5.GetOfTrue() > t12.GetOfTrue());
        }
        
        [Fact]
        public void testCltDouble(){
            Code code = Code.Clt;
            
            Truthness t3 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(0.3d, 0d, code);
            Assert.False(t3.IsTrue());
            Assert.True(t3.IsFalse());

            Truthness t5 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(0.5d, 0d, code);
            Assert.False(t5.IsTrue());
            Assert.True(t5.IsFalse());

            Truthness t12 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(1.2d, 0d, code);
            Assert.False(t12.IsTrue());
            Assert.True(t12.IsFalse());

            Assert.True(t5.GetOfTrue() < t3.GetOfTrue());
            Assert.True(t5.GetOfTrue() > t12.GetOfTrue());
        }
        
        [Fact]
        public void testCltLong(){
            Code code = Code.Clt;
            
            Truthness t3 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(3L, 0L, code);
            Assert.False(t3.IsTrue());
            Assert.True(t3.IsFalse());

            Truthness t5 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(5L, 0L, code);
            Assert.False(t5.IsTrue());
            Assert.True(t5.IsFalse());

            Truthness t12 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(12L, 0L, code);
            Assert.False(t12.IsTrue());
            Assert.True(t12.IsFalse());

            Assert.True(t5.GetOfTrue() < t3.GetOfTrue());
            Assert.True(t5.GetOfTrue() > t12.GetOfTrue());
        }
        
        
        [Fact]
        public void testCgtFloat(){
            Code code = Code.Cgt;
            
            Truthness t3 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-0.3f, 0f, code);
            Assert.False(t3.IsTrue());
            Assert.True(t3.IsFalse());

            Truthness t5 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-0.5f, 0f, code);
            Assert.False(t5.IsTrue());
            Assert.True(t5.IsFalse());

            Truthness t12 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-1.2f, 0f, code);
            Assert.False(t12.IsTrue());
            Assert.True(t12.IsFalse());

            Assert.True(t5.GetOfTrue() < t3.GetOfTrue());
            Assert.True(t5.GetOfTrue() > t12.GetOfTrue());
        }
        
        [Fact]
        public void testCgtDouble(){
            Code code = Code.Cgt;
            
            Truthness t3 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-0.3d, 0d, code);
            Assert.False(t3.IsTrue());
            Assert.True(t3.IsFalse());

            Truthness t5 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-0.5d, 0d, code);
            Assert.False(t5.IsTrue());
            Assert.True(t5.IsFalse());

            Truthness t12 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-1.2d, 0d, code);
            Assert.False(t12.IsTrue());
            Assert.True(t12.IsFalse());

            Assert.True(t5.GetOfTrue() < t3.GetOfTrue());
            Assert.True(t5.GetOfTrue() > t12.GetOfTrue());
        }
        
        [Fact]
        public void testCgtLong(){
            Code code = Code.Cgt;
            
            Truthness t3 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-3L, 0L, code);
            Assert.False(t3.IsTrue());
            Assert.True(t3.IsFalse());

            Truthness t5 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-5L, 0L, code);
            Assert.False(t5.IsTrue());
            Assert.True(t5.IsFalse());

            Truthness t12 = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(-12L, 0L, code);
            Assert.False(t12.IsTrue());
            Assert.True(t12.IsFalse());

            Assert.True(t5.GetOfTrue() < t3.GetOfTrue());
            Assert.True(t5.GetOfTrue() > t12.GetOfTrue());
        }

        [Theory]
        [InlineData(-100d)]
        [InlineData(-42.42d)]
        [InlineData(42.42d)]
        [InlineData(100d)]
        public void testDouble(double first){
            Truthness gt = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(first, 0d, Code.Cgt);
            Truthness le = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(first, 0d, Code.Clt);

            //their values should be inverted
            Assert.Equal(gt.IsTrue(), le.IsFalse());
            Assert.Equal(gt.IsFalse(), le.IsTrue());
        }
        
        [Theory]
        [InlineData(-2345L)]
        [InlineData(-63L)]
        [InlineData(211)]
        [InlineData(7888)]
        public void testLong(long first){
            Truthness gt = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(first, 0L, Code.Cgt);
            Truthness le = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(first, 0L, Code.Clt);

            //their values should be inverted
            Assert.Equal(gt.IsTrue(), le.IsFalse());
            Assert.Equal(gt.IsFalse(), le.IsTrue());
        }
    }
}