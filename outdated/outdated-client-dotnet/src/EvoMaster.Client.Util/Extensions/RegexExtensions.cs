using System.Text.RegularExpressions;

namespace EvoMaster.Client.Util.Extensions {
    public static class RegexExtensions {
        public static bool IsEntirelyMatch(this Regex regex, string input) {
            var matches = regex.Matches(input);

            return matches.Count == 1 && matches[0].ToString() == input.Trim();
        }
    }
}