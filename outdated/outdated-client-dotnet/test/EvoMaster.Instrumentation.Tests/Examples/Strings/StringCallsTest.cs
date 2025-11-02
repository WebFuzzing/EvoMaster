using System;
using EvoMaster.Instrumentation.Examples.strings;
using EvoMaster.Instrumentation.Examples.Strings;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Strings{
    [Collection("Sequential")]
    public class StringCallsTest{
        private IStringCalls getInstance() => new StringCallsImp();

        [Fact]
        public void test_equals_firstNull(){
            IStringCalls sc = getInstance();
            Assert.Throws<NullReferenceException>(() => sc.callEquals(null, "foo"));
        }

        [Fact]
        public void test_equals_secondNull(){
            IStringCalls sc = getInstance();
            Assert.False(sc.callEquals("foo", null));
        }

        [Fact]
        public void test_equals_true(){
            IStringCalls sc = getInstance();
            Assert.True(sc.callEquals("foo", "foo"));
        }

        [Fact]
        public void test_equals_false(){
            IStringCalls sc = getInstance();
            Assert.False(sc.callEquals("foo", "bar"));
        }

        // [Fact]
        // public void test_equals_noToString() {
        //     IStringCalls sc = getInstance();
        //     string a = "foo";
        //     StringBuffer b = new StringBuffer(a);
        //
        //     Assert.True(sc.callEquals(a, b.toString()));
        //     Assert.False(sc.callEquals(a, b));
        // }

        [Fact]
        public void test_equalsIgnoreCase_firstNull(){
            IStringCalls sc = getInstance();
            Assert.Throws<NullReferenceException>(() =>
                sc.callEqualsStringComparison(null, "foo", StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_equalsIgnoreCase_secondNull(){
            IStringCalls sc = getInstance();
            Assert.False(sc.callEqualsStringComparison("foo", null, StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_equalsIgnoreCase_true(){
            IStringCalls sc = getInstance();
            Assert.True(sc.callEqualsStringComparison("FoO", "fOo", StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_equalsIgnoreCase_false(){
            IStringCalls sc = getInstance();
            Assert.False(sc.callEqualsStringComparison("foo", "bar", StringComparison.OrdinalIgnoreCase));
        }


        [Fact]
        public void test_StartsWith_firstNull(){
            IStringCalls sc = getInstance();
            Assert.Throws<NullReferenceException>(() => sc.callStartsWith(null, "foo"));
        }

        [Fact]
        public void test_StartsWith_secondNull(){
            IStringCalls sc = getInstance();
            Assert.Throws<ArgumentNullException>(() => sc.callStartsWith("foo", null));
        }

        [Fact]
        public void test_StartsWith_true(){
            IStringCalls sc = getInstance();
            Assert.True(sc.callStartsWith("foo", "f"));
        }

        [Fact]
        public void test_StartsWith_firstNull_ignore(){
            IStringCalls sc = getInstance();
            Assert.Throws<NullReferenceException>(() =>
                sc.callStartsWithStringComparison(null, "foo", StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_StartsWith_secondNull_ignore(){
            IStringCalls sc = getInstance();
            Assert.Throws<ArgumentNullException>(() =>
                sc.callStartsWithStringComparison("foo", null, StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_StartsWith_true_ignore(){
            IStringCalls sc = getInstance();
            Assert.True(sc.callStartsWithStringComparison("foo", "F", StringComparison.OrdinalIgnoreCase));
            Assert.True(sc.callStartsWithStringComparison("foo", "FO", StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_StartsWith_false(){
            IStringCalls sc = getInstance();
            Assert.False(sc.callStartsWith("foo", "bar"));
            Assert.False(sc.callStartsWith("f", "bar"));
        }

        // [Fact]
        // public void test_StartsWith_offset() {
        //     IStringCalls sc = getInstance();
        //     Assert.True(sc.callStartsWith("foo", "o", 1));
        //     Assert.True(sc.callStartsWith("foo", "o", 2));
        //     Assert.False(sc.callStartsWith("foo", "o", -1));
        //     Assert.False(sc.callStartsWith("foo", "o", 0));
        //     Assert.False(sc.callStartsWith("foo", "o", 3));
        // }

        [Fact]
        public void test_EndsWith_firstNull(){
            IStringCalls sc = getInstance();
            Assert.Throws<NullReferenceException>(() => sc.callEndsWith(null, "foo"));
        }

        [Fact]
        public void test_EndsWith_secondNull(){
            IStringCalls sc = getInstance();
            Assert.Throws<ArgumentNullException>(() => sc.callEndsWith("foo", null));
        }

        [Fact]
        public void test_EndsWith_firstNull_ignore(){
            IStringCalls sc = getInstance();
            Assert.Throws<NullReferenceException>(() =>
                sc.callEndsWithStringComparison(null, "foo", StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_EndsWith_secondNull_ignore(){
            IStringCalls sc = getInstance();
            Assert.Throws<ArgumentNullException>(() =>
                sc.callEndsWithStringComparison("foo", null, StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_EndsWith_true(){
            IStringCalls sc = getInstance();
            Assert.True(sc.callEndsWith("foo", "o"));
        }

        [Fact]
        public void test_EndsWith_true_ignore(){
            IStringCalls sc = getInstance();
            Assert.True(sc.callEndsWithStringComparison("foo", "O", StringComparison.OrdinalIgnoreCase));
            Assert.True(sc.callEndsWithStringComparison("foo", "OO", StringComparison.OrdinalIgnoreCase));
            Assert.True(sc.callEndsWithStringComparison("foo", "FOO", StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_EndsWith_false(){
            IStringCalls sc = getInstance();
            Assert.False(sc.callEndsWith("foo", "f"));
        }

        [Fact]
        public void test_Contains_firstNull(){
            IStringCalls sc = getInstance();
            Assert.Throws<NullReferenceException>(() => sc.callContains(null, "foo"));
        }

        [Fact]
        public void test_Contains_secondNull(){
            IStringCalls sc = getInstance();
            Assert.Throws<ArgumentNullException>(() => sc.callContains("foo", null));
        }

        [Fact]
        public void test_Contains_firstNull_ignore(){
            IStringCalls sc = getInstance();
            Assert.Throws<NullReferenceException>(() =>
                sc.callContainsStringComparison(null, "foo", StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_Contains_secondNull_ignore(){
            IStringCalls sc = getInstance();
            Assert.Throws<ArgumentNullException>(() =>
                sc.callContainsStringComparison("foo", null, StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_Contains_true(){
            IStringCalls sc = getInstance();
            Assert.True(sc.callContains("foo", ""));
            Assert.True(sc.callContains("foo", "f"));
            Assert.True(sc.callContains("foo", "fo"));
            Assert.True(sc.callContains("foo", "foo"));
            Assert.True(sc.callContains("foo", "oo"));
            Assert.True(sc.callContains("foo", "o"));
        }

        [Fact]
        public void test_Contains_true_ignore(){
            IStringCalls sc = getInstance();
            Assert.True(sc.callContainsStringComparison("foo", "", StringComparison.OrdinalIgnoreCase));
            Assert.True(sc.callContainsStringComparison("foo", "F", StringComparison.OrdinalIgnoreCase));
            Assert.True(sc.callContainsStringComparison("foo", "FO", StringComparison.OrdinalIgnoreCase));
            Assert.True(sc.callContainsStringComparison("foo", "FoO", StringComparison.OrdinalIgnoreCase));
            Assert.True(sc.callContainsStringComparison("foo", "Oo", StringComparison.OrdinalIgnoreCase));
            Assert.True(sc.callContainsStringComparison("foo", "O", StringComparison.OrdinalIgnoreCase));
        }

        [Fact]
        public void test_Contains_false(){
            IStringCalls sc = getInstance();
            Assert.False(sc.callContains("foo", "bar"));
            Assert.False(sc.callContains("foo", "foooo"));
        }
    }
}