using EvoMaster.Instrumentation.Examples.Triangle;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle {
    public class TriangleClassificationTest {
        //This test is added in order to make sure everything works as before after the instrumentation
        [Theory]
        [InlineData(-1, 0, 0, Classification.NOT_A_TRIANGLE)]
        [InlineData(10, 11, 1, Classification.NOT_A_TRIANGLE)]
        [InlineData(6, 6, 6, Classification.EQUILATERAL)]
        [InlineData(7, 6, 7, Classification.ISOSCELES)]
        [InlineData(7, 6, 5, Classification.SCALENE)]
        public void TestFunctionality(int a, int b, int c, Classification expectedOutcome) {
            ITriangleClassification tc = new TriangleClassificationImpl();

            var res = tc.Classify(a,b,c);
            
            Assert.Equal(expectedOutcome, res);
        }
    }
}