using System;
using EvoMaster.Instrumentation_Shared;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Coverage.MethodReplacement {
    public class TaintInputNameTest {
        
        [Fact]
        public void TestBase(){

            var name = TaintInputName.GetTaintName(0);
            Assert.True(TaintInputName.IsTaintInput(name));
        }

        [Fact]
        public void TestNegativeId(){
            Assert.Throws<ArgumentException>(() =>  TaintInputName.GetTaintName(-1));
        }
        
        [Fact]
        public void TestInvalidNames() {
            Assert.False(TaintInputName.IsTaintInput("foo"));
            Assert.False(TaintInputName.IsTaintInput(""));
            Assert.False(TaintInputName.IsTaintInput("evomaster"));
            Assert.False(TaintInputName.IsTaintInput("evomaster_input"));
            Assert.False(TaintInputName.IsTaintInput("evomaster__input"));
            Assert.False(TaintInputName.IsTaintInput("evomaster_a_input"));
            Assert.True(TaintInputName.IsTaintInput("_EM_42_XYZ_"));
        }
        
        [Fact]
        public void TestInvalidNamePatterns(){
            var prefix = "_EM_";
            var postfix = "_XYZ_";

            Assert.False(TaintInputName.IsTaintInput("foo"));
            Assert.False(TaintInputName.IsTaintInput(""));
            Assert.False(TaintInputName.IsTaintInput(prefix));
            Assert.False(TaintInputName.IsTaintInput(prefix + postfix));
            Assert.False(TaintInputName.IsTaintInput(prefix+"a"+postfix));

            Assert.True(TaintInputName.IsTaintInput(prefix+"42"+postfix));
        }


        [Fact]
        public void TestIncludes(){

            var name = TaintInputName.GetTaintName(0);
            var text = "some prefix " + name + " some postfix";

            Assert.False(TaintInputName.IsTaintInput(text));
            Assert.True(TaintInputName.IncludesTaintInput(text));
        }
        
        [Fact]
        public void TestUpperLowerCase(){

            String name = TaintInputName.GetTaintName(0);

            Assert.True(TaintInputName.IsTaintInput(name));
            Assert.True(TaintInputName.IncludesTaintInput(name));


            Assert.True(TaintInputName.IsTaintInput(name.ToLower()));
            Assert.True(TaintInputName.IncludesTaintInput(name.ToLower()));
            Assert.True(TaintInputName.IsTaintInput(name.ToUpper()));
            Assert.True(TaintInputName.IncludesTaintInput(name.ToUpper()));
        }
    }
}