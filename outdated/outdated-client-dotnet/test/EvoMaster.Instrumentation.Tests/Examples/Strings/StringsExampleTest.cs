using EvoMaster.Instrumentation.Examples.Strings;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Strings{
    [Collection("Sequential")]
    public class StringsExampleTest{
        //protected abstract IStringsExample getInstance() ;

        private IStringsExample getInstance() => new StringsExampleImp();

        [Fact]
        public void test_isFooWithDirectReturn(){
            IStringsExample se = getInstance();
            Assert.True(se.isFooWithDirectReturn("foo"));
            Assert.False(se.isFooWithDirectReturn("bar"));
        }

        // [Fact]
        // public void test_isFooWithDirectReturnUsingReplacement() {
        //
        //     IStringsExample se = getInstance();
        //     Assert.True(se.isFooWithDirectReturnUsingReplacement("foo"));
        //     Assert.False(se.isFooWithDirectReturnUsingReplacement("bar"));
        // }

        [Fact]
        public void test_isFooWithBooleanCheck(){
            IStringsExample se = getInstance();
            Assert.True(se.isFooWithBooleanCheck("foo"));
            Assert.False(se.isFooWithBooleanCheck("bar"));
        }

        [Fact]
        public void test_isFooWithNegatedBooleanCheck(){
            IStringsExample se = getInstance();
            Assert.True(se.isFooWithNegatedBooleanCheck("foo"));
            Assert.False(se.isFooWithNegatedBooleanCheck("bar"));
        }


        [Fact]
        public void test_isFooWithIf(){
            IStringsExample se = getInstance();
            Assert.True(se.isFooWithIf("foo"));
            Assert.False(se.isFooWithIf("bar"));
        }

        [Fact]
        public void test_isFooWithLocalVariable(){
            IStringsExample se = getInstance();
            Assert.True(se.isFooWithLocalVariable("foo"));
            Assert.False(se.isFooWithLocalVariable("bar"));
        }

        [Fact]
        public void test_isFooWithLocalVariableInIf(){
            IStringsExample se = getInstance();
            Assert.True(se.isFooWithLocalVariableInIf("foo"));
            Assert.False(se.isFooWithLocalVariableInIf("bar"));
        }

        [Fact]
        public void test_isNotFooWithLocalVariable(){
            IStringsExample se = getInstance();
            Assert.False(se.isNotFooWithLocalVariable("foo"));
            Assert.True(se.isNotFooWithLocalVariable("bar"));
        }

        [Fact]
        public void test_isBarWithPositiveX(){
            IStringsExample se = getInstance();
            Assert.True(se.isBarWithPositiveX("bar", 5));
            Assert.False(se.isBarWithPositiveX("bar", -5));
            Assert.False(se.isBarWithPositiveX("foo", 5));
        }
    }
}