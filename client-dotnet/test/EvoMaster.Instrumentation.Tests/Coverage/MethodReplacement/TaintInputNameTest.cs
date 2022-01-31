using EvoMaster.Instrumentation_Shared;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Coverage.MethodReplacement {
    public class TaintInputNameTest {
        [Fact]
        public void TestInvalidNames() {
            //TODO
            Assert.False(TaintInputName.IsTaintInput("foo"));
            Assert.False(TaintInputName.IsTaintInput(""));
            Assert.False(TaintInputName.IsTaintInput("evomaster"));
            Assert.False(TaintInputName.IsTaintInput("evomaster_input"));
            Assert.False(TaintInputName.IsTaintInput("evomaster__input"));
            Assert.False(TaintInputName.IsTaintInput("evomaster_a_input"));
            Assert.True(TaintInputName.IsTaintInput("_EM_42_XYZ_"));
        }
    }
}