using EvoMaster.Instrumentation.Examples.Strings;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Strings {
    [Collection("Sequential")]
    public class StringOperationsTests {
        private StringOperations _stringOperations = new StringOperations();
        
        [Theory]
        [InlineData("I Go To School By Bus","I Go To School By Bus","I Go To School By Bus==I Go To School By Bus")]
        [InlineData("I Go To School By Bus","It's a Blackboard","I Go To School By Bus!=It's a Blackboard")]
        public void CheckEqualityTest(string a, string b, string expectedResult) {
            var actualResult = _stringOperations.CheckEquality(a, b);
            
            Assert.Equal(expectedResult,actualResult);
        }
        
        [Theory]
        [InlineData("Jeg bor i Norge","Jeg bor i Norge",true)]
        [InlineData("PIZZA","pizza",false)]
        [InlineData("PIZZA","PIZZA",true)]
        public void CheckEqualsTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckEquals(a, b);
            
            Assert.Equal(expectedResult,actualResult);
        }
        
        [Theory]
        [InlineData("I Go To School By Bus","I Go To School By Bus",true)]
        [InlineData("PIZZA","pizza",true)]
        [InlineData("PIZZA", "PIZZA", true)]
        [InlineData("abcde", "abcd", false)]
        public void CheckEqualsTestWithOrdinalIgnoreCase(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckEqualsWithOrdinalIgnoreCase(a, b);
            
            Assert.Equal(expectedResult,actualResult);
        }
    }
}