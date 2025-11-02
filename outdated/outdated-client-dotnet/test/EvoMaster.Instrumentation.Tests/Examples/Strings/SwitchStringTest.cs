using EvoMaster.Instrumentation.Examples.Strings;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Strings{
    [Collection("Sequential")]
    public class SwitchStringTest{
        
        private SwitchString getInstance() => new SwitchString();
        
        [Theory]
        [InlineData("one", 1)]
        [InlineData("two", 2)]
        [InlineData("three", 3)]
        [InlineData("foo", 0)]
        public void CheckSwithString(string a, int expectedResult){
            var actualResult = getInstance().switchString(a);
            Assert.Equal(expectedResult, actualResult);
        }
    }
}