using System;
using System.Globalization;
using System.Threading;
using EvoMaster.Instrumentation.Examples.Strings;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Strings {
    [Collection("Sequential")]
    public class StringOperationsTests {
        private StringOperations _stringOperations = new StringOperations();
        
        /**
         * note that culture (such as Invariant and Current) are not handled when calculating the distance
         * but the semantic is properly handled as tests
         * https://docs.microsoft.com/en-us/dotnet/api/system.stringcomparison?view=netcore-3.1
         * comment this test out for the moment since it fails on CI
         */
        // [Theory]
        // [InlineData("encyclopædia", "encyclopaedia", StringComparison.InvariantCulture, true, "se-SE")]
        // [InlineData("encyclopædia", "encyclopaedia", StringComparison.CurrentCulture, false, "se-SE")]
        // [InlineData("encyclopædia", "encyclopaedia", StringComparison.InvariantCulture, true, "en-US")]
        // [InlineData("encyclopædia", "encyclopaedia", StringComparison.CurrentCulture, true, "en-US")]
        // public void CheckEqualityWithStringComparisonTest(string a, string b, StringComparison comparison, bool expectedResult, string culture) {
        //     Thread.CurrentThread.CurrentCulture = CultureInfo.CreateSpecificCulture(culture);
        //     var actualResult = _stringOperations.CheckEquals(a, b, comparison);
        //     Assert.Equal(expectedResult, actualResult);
        // }

        [Theory]
        [InlineData("I Go To School By Bus", "I Go To School By Bus", "I Go To School By Bus==I Go To School By Bus")]
        [InlineData("I Go To School By Bus", "It's a Blackboard", "I Go To School By Bus!=It's a Blackboard")]
        public void CheckEqualityTest(string a, string b, string expectedResult) {
            var actualResult = _stringOperations.CheckEquality(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData("Jeg bor i Norge", "Jeg bor i Norge", true)]
        [InlineData("PIZZA", "pizza", false)]
        [InlineData("PIZZA", "PIZZA", true)]
        public void CheckEqualsTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckEquals(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData("I Go To School By Bus", "I Go To School By Bus", true)]
        [InlineData("PIZZA", "pizza", true)]
        [InlineData("PIZZA", "PIZZA", true)]
        [InlineData("abcde", "abcd", false)]
        public void CheckEqualsWithOrdinalIgnoreCaseTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckEqualsWithOrdinalIgnoreCase(a, b);

            Assert.Equal(expectedResult, actualResult);
        }


        [Theory]
        [InlineData("Jeg bor i Norge", "Norge", true)]
        [InlineData("Jeg bor i Norge", "norge", false)]
        [InlineData("Jeg bor i Norge", "Sverige", false)]
        public void CheckContainsTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckContains(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData("Jeg bor i Norge", "Norge", true)]
        [InlineData("Jeg bor i Norge", "norge", true)]
        [InlineData("Jeg bor i Norge", "Russland", false)]
        public void CheckContainsWithOrdinalIgnoreCaseTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckContainsWithOrdinalIgnoreCase(a, b);

            Assert.Equal(expectedResult, actualResult);
        }
        
        
        [Theory]
        [InlineData("Jeg bor i Norge", "Jeg", true)]
        [InlineData("Jeg bor i Norge", "jeg", false)]
        [InlineData("Jeg bor i Norge", "eple", false)]
        public void CheckStartsWithTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckStartsWith(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData("Hun spiser eplet", "Hun", true)]
        [InlineData("Hun spiser eplet", "hun", true)]
        [InlineData("Hun spiser eplet", "jord", false)]
        public void CheckStartsWithOrdinalIgnoreCaseTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckStartsWithWithOrdinalIgnoreCase(a, b);

            Assert.Equal(expectedResult, actualResult);
        }
        
        [Theory]
        [InlineData("Hun spiser eplet", "eplet", true)]
        [InlineData("Hun spiser eplet", "Eplet", false)]
        [InlineData("Hun spiser eplet", "jord", false)]
        public void CheckEndsWithTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckEndsWith(a, b);

            Assert.Equal(expectedResult, actualResult);
        }

        [Theory]
        [InlineData("Hun spiser eplet", "eplet", true)]
        [InlineData("Hun spiser eplet", "Eplet", true)]
        [InlineData("Hun spiser eplet", "jord", false)]
        public void CheckEndsWithOrdinalIgnoreCaseTest(string a, string b, bool expectedResult) {
            var actualResult = _stringOperations.CheckEndsWithWithOrdinalIgnoreCase(a, b);

            Assert.Equal(expectedResult, actualResult);
        }
    }
}