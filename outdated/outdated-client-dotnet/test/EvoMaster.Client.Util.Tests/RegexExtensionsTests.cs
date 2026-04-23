using System;
using System.Text.RegularExpressions;
using EvoMaster.Client.Util.Extensions;
using Xunit;

namespace EvoMaster.Client.Util.Tests
{
    public class RegexExtensionsTests {
        private const string EmailPattern =
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

        private const string DatePattern =
            "^(0[1-9]|[12][0-9]|3[01])[- /.](0[1-9]|1[012])[- /.](19|20)\\d\\d$"; //Format: DD/MM/YYYY
        
        [Theory]
        //match email. The input is valid as it only contains only one valid email address
        [InlineData(EmailPattern,"someone@email.com")]
        [InlineData(DatePattern,"19/09/2021")]
        [InlineData(DatePattern,"07/04/1972")]
        public void IsEntirelyMatch_ValidInput_ReturnsTrue(string pattern, string input) {
            var regex = new Regex(pattern);
            var res = regex.IsEntirelyMatch(input);
            Assert.True(res);
        }
        
        [Theory]
        //input contains two valid emails which is invalid in this case as it has to be only one
        [InlineData(EmailPattern,"someone@email.com somebody@email.com")]
        [InlineData(EmailPattern,"s omeone@email.com")]
        [InlineData(DatePattern,"07/04/2004 19/09/2021")]
        [InlineData(DatePattern,"02/8/2004")]
        [InlineData(DatePattern,"02/13/2004")]
        [InlineData(DatePattern,"17/04/2004 x")]
        public void IsEntirelyMatch_InValidInput_ReturnsFalse(string pattern, string input) {
            var regex = new Regex(pattern);
            var res = regex.IsEntirelyMatch(input);
            Assert.False(res);
        }
    }
}
