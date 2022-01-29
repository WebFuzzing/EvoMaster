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

        [Theory]
        [InlineData(6, 3)]
        public void TestCompareWithInfinite(int a, int b) {
            Assert.Throws<DivideByZeroException>(() => _numericOperations.CompareWithInfinite(a, b));
        }

        [Theory]
        [InlineData(12, 3, 4)]
        [InlineData(121, 11, 11)]
        [InlineData(100, 0, int.MaxValue)]
        public void TestDivie(int a, int b, int expectedResult) {
            var actualResult = _numericOperations.Divide(a, b);
            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData(1.0, 2.5, 1.0)]
        [InlineData(2.0, 1.5, 1.5)]
        [InlineData(1.0, 0.0/0.0, 1.0)]
        public void TestCgtUnDouble(double a, double x, double expectedResult){
            var actualResult = _numericOperations.CgtUnDouble(a, x);
            Assert.Equal(expectedResult, actualResult);
        }
        
        [Theory]
        [InlineData(0.0/0.0, 1.5, 0.0/0.0)]
        public void TestCgtUnNAN(double a, double x, double expectedResult){
            var actualResult = _numericOperations.CgtUnDouble(a, x);
            Assert.True(double.IsNaN(actualResult));
        }

        [Theory]
        [InlineData(-1.0, 0)]
        public void TestThrows(double a, double x){
            Assert.Throws<Exception>(() => _numericOperations.CgtUnDouble(a, x));
        }
    }
}