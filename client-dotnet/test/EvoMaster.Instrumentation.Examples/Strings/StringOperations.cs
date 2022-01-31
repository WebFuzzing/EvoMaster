using System;

namespace EvoMaster.Instrumentation.Examples.Strings {
    public class StringOperations {
        public string CheckEquality(string a, string b) {
            if (a == b) {
                return $"{a}=={b}";
            }

            return $"{a}!={b}";
        }

        public bool CheckEquals(string a, string b) {
            if (a.Equals(b)) {
                return true;
            }

            return false;
        }

        public bool CheckEqualsWithOrdinalIgnoreCase(string a, string b) {
            if (a.Equals(b, StringComparison.OrdinalIgnoreCase)) {
                return true;
            }

            return false;
        }
    }
}