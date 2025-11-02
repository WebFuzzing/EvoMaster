using Xunit;

namespace EvoMaster.Instrumentation.Tests.Heuristic{
    public class TruthnessUtilsTest{

        [Theory]
        [InlineData(-5E-324, 0)]
        [InlineData(double.Epsilon, 0)]
        public void TestDoubleEqualityTruthness(double a, double b){
            var truthness = TruthnessUtils.GetEqualityTruthness(a, b);
            Assert.True(truthness.IsFalse());
            Assert.False(truthness.IsTrue());
        }
    }
}