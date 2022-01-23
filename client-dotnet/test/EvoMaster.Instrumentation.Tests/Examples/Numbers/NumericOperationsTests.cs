using System.Threading;
using EvoMaster.Instrumentation.Examples.Numbers;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Numbers {
    public class NumericOperationsTests {
        private NumericOperations _numericOperations;

        public NumericOperationsTests() {
            _numericOperations = new NumericOperations();
        }

        [Theory]
        [InlineData(8, 7, true)]
        [InlineData(4, 4400, false)]
        [InlineData(99, 99, false)]
        public void TestGreaterThan(double a, double b, bool expectedResult) {
            var actualResult = _numericOperations.GreaterThan(a, b);

            Assert.Equal(expectedResult, actualResult);
        }
    }
}