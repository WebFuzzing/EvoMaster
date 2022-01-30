using System;
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
        [InlineData(0.0 / 0.0, 0.0 / 0.0, false)]
        [InlineData(0.0 / 0.0, 6, false)]
        [InlineData(-6, 0.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 1.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 6, true)]
        [InlineData(-1.0 / 0.0, 6, false)]
        [InlineData(-6, 1.0 / 0.0, false)]
        public void TestGreaterThanForDouble(double a, double b, bool expectedResult) {
            var actualResult = _numericOperations.GreaterThan(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(8, 7, true)]
        [InlineData(4, 4400, false)]
        [InlineData(99, 99, false)]
        [InlineData(0.0 / 0.0, 0.0 / 0.0, false)]
        [InlineData(0.0 / 0.0, 6, false)]
        [InlineData(-6, 0.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 1.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 6, true)]
        [InlineData(-1.0 / 0.0, 6, false)]
        [InlineData(-6, 1.0 / 0.0, false)]
        public void TestGreaterThanForFloat(float a, float b, bool expectedResult) {
            var actualResult = _numericOperations.GreaterThan(a, b);

            Assert.Equal(expectedResult, actualResult);
        }
        
        [Theory]
        [InlineData(8, 17, true)]
        [InlineData(4, 4400, true)]
        [InlineData(99, 99, false)]
        [InlineData(0.0 / 0.0, 0.0 / 0.0, false)]
        [InlineData(0.0 / 0.0, 6, false)]
        [InlineData(-6, 0.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 0.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 6, false)]
        [InlineData(-6, 1.0 / 0.0, true)]
        [InlineData(-6, -1.0 / 0.0, false)]
        public void TestLowerThanForDouble(double a, double b, bool expectedResult) {
            var actualResult = _numericOperations.LowerThan(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(8, 17, true)]
        [InlineData(4, 4400, true)]
        [InlineData(99, 99, false)]
        [InlineData(0.0 / 0.0, 0.0 / 0.0, false)]
        [InlineData(0.0 / 0.0, 6, false)]
        [InlineData(-6, 0.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 1.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 6, false)]
        [InlineData(-6, 1.0 / 0.0, true)]
        [InlineData(-6, -1.0 / 0.0, false)]
        public void TestLowerThanForFloat(float a, float b, bool expectedResult) {
            var actualResult = _numericOperations.LowerThan(a, b);

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

        [Theory]
        [InlineData(6, 3)]
        public void TestCompareWithInfinite(int a, int b) {
            Assert.Throws<DivideByZeroException>(() => _numericOperations.CompareWithInfinite(a, b));
        }

        [Theory]
        [InlineData(12, 3, 4)]
        [InlineData(121, 11, 11)]
        [InlineData(100, 0, int.MaxValue)]
        public void TestDivide(int a, int b, int expectedResult) {
            var actualResult = _numericOperations.Divide(a, b);
            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(1.0, 2.5, 1.0)]
        [InlineData(2.0, 1.5, 1.5)]
        [InlineData(1.0, 0.0 / 0.0, 1.0)]
        [InlineData(1.0, 1.0 / 0.0, 1.0)]
        public void TestCgtUnDouble(double a, double x, double expectedResult) {
            var actualResult = _numericOperations.CgtUnDouble(a, x);
            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(0.0 / 0.0, 1.5)]
        public void TestCgtUnNan(double a, double x) {
            var actualResult = _numericOperations.CgtUnDouble(a, x);
            Assert.True(double.IsNaN(actualResult));
        }
        
        [Theory]
        [InlineData(1.0 / 0.0, 1.5)]
        public void TestCgtUnInfinity(double a, double x) {
            var actualResult = _numericOperations.CgtUnDouble(a, x);
            Assert.True(double.IsFinite(actualResult));
        }

        [Theory]
        [InlineData(-1.0, 0)]
        public void TestThrows(double a, double x) {
            Assert.Throws<Exception>(() => _numericOperations.CgtUnDouble(a, x));
        }
        

        [Theory]
        [InlineData(1.0, 2.5, false)]
        [InlineData(666, 666, true)]
        [InlineData(0.0 / 0.0, 1.5, false)]
        [InlineData(0.0 / 0.0, 0.0 / 0.0, false)] //compare NaN with NaN
        [InlineData(89, 0.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 1.5, false)]
        [InlineData(89, 1.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 1.0 / 0.0, true)] //note that compare positive infinity with positive infinity, result in true
        [InlineData(-1.0 / 0.0, -1.0 / 0.0, true)] //note that compare negative infinity with negative infinity, result in true
        [InlineData(-1.0 / 0.0, 1.0 / 0.0, false)] //note that compare negative infinity with positive infinity, result in true
        public void TestAreEqual_Double(double a, double b, bool expectedResult) {
            var actualResult = _numericOperations.AreEqual(a, b);
            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(1.0, 2.5, false)]
        [InlineData(666, 666, true)]
        [InlineData(0.0 / 0.0, 1.5, false)]
        [InlineData(0.0 / 0.0, 0.0 / 0.0, false)] //compare NaN with NaN
        [InlineData(89, 0.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 1.5, false)]
        [InlineData(89, 1.0 / 0.0, false)]
        [InlineData(1.0 / 0.0, 1.0 / 0.0, true)] //note that compare positive infinity with positive infinity, result in true
        [InlineData(-1.0 / 0.0, -1.0 / 0.0, true)] //note that compare negative infinity with negative infinity, result in true
        [InlineData(-1.0 / 0.0, 1.0 / 0.0, false)] //note that compare negative infinity with positive infinity, result in true
        public void TestAreEqual_Float(float a, float b, bool expectedResult) {
            var actualResult = _numericOperations.AreEqual(a, b);
            Assert.Equal(expectedResult, actualResult);
        }
    }
}