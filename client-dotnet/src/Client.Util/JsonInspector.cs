using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;

namespace Client.Util
{
    public class JsonInspector
    {
        public static bool ContainsKeyValue<T>(string json, string key, T value)
        {
            if (string.IsNullOrWhiteSpace(key))
                throw new ArgumentNullException("Title");

            var matches = GetMatches(json, key);

            if (!matches.Any())
            {
                key = ToOppositeCase(key);

                matches = GetMatches(json, key);
            }

            return matches.Any(x => x.Equals(value.ToString()));
        }

        private static ICollection<string> GetMatches(string json, string key)
        {
            var regex = new Regex($"(?<=\"{key}\"\\ *:\\ *\")(?:\\\\\"|[^\"])*");

            return regex.Matches(json).Cast<Match>().Select(x => x.Value).ToList();
        }
        private static string ToOppositeCase(string value)
        {
            if (char.IsUpper(value[0]))
                return char.ToLowerInvariant(value[0]) + value.Substring(1);
            return char.ToUpperInvariant(value[0]) + value.Substring(1);
        }
    }
}