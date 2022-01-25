using System.Threading;
using EvoMaster.Instrumentation.Examples.Numbers;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Numbers {
    [Collection("Sequential")]
    public class NumericOperationsTests {
                private readonly NumericOperations _numericOperations = new NumericOperations();

        [Theory]
        [InlineData(8, 7, true)]
        [InlineData(4, 4400, false)]
        [InlineData(99, 99, false)]
        public void TestGreaterThanForDouble(double a, double b, bool expectedResult) {
            var actualResult = _numericOperations.GreaterThan(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(8, 7, true)]
        [InlineData(4, 4400, false)]
        [InlineData(99, 99, false)]
        public void TestGreaterThanForFloat(float a, float b, bool expectedResult) {
            var actualResult = _numericOperations.GreaterThan(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(8, 7, true)]
        [InlineData(4, 4400, false)]
        [InlineData(9, 9, false)]
        public void TestGreaterThanForLong(long a, long b, bool expectedResult) {
            var actualResult = _numericOperations.GreaterThan(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(8, 7, true)]
        [InlineData(4, 4400, false)]
        [InlineData(9, 9, false)]
        public void TestGreaterThanForShort(short a, short b, bool expectedResult) {
            var actualResult = _numericOperations.GreaterThan(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(10, 1)]
        [InlineData(7, 0)]
        [InlineData(2, -1)]
        public void TestCompareWithLocalVariable(int a, int expectedResult) {
            var actualResult = _numericOperations.CompareWithLocalVariable(a);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(10, 1)]
        [InlineData(7, 0)]
        [InlineData(2, -1)]
        public void TestCompareWithGlobalVariable(int a, int expectedResult) {
            var actualResult = _numericOperations.CompareWithGlobalVariable(a);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(10, 1)]
        [InlineData(7, 0)]
        [InlineData(2, -1)]
        public void TestCompareWithStaticVariable(int a, int expectedResult) {
            var actualResult = _numericOperations.CompareWithStaticVariable(a);

            Assert.Equal(expectedResult, actualResult);
        }

        [Fact]
        public void TestCompareTwoGlobalVariables() {
            var actualResult = _numericOperations.CompareTwoGlobalVariables();

            Assert.Equal(1, actualResult);
        }
    }
}