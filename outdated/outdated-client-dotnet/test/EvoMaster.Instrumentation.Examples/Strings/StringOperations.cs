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
        
        public bool CheckEquals(string a, string b, StringComparison comparison) {
            if (a.Equals(b, comparison)) {
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
        
        public bool CheckContains(string a, string b) {
            if (a.Contains(b)) {
                return true;
            }

            return false;
        }

        public bool CheckContainsWithOrdinalIgnoreCase(string a, string b) {
            if (a.Contains(b, StringComparison.OrdinalIgnoreCase)) {
                return true;
            }

            return false;
        }
        
        public bool CheckStartsWith(string a, string b) {
            if (a.StartsWith(b)) {
                return true;
            }

            return false;
        }

        public bool CheckStartsWithWithOrdinalIgnoreCase(string a, string b) {
            if (a.StartsWith(b, StringComparison.OrdinalIgnoreCase)) {
                return true;
            }

            return false;
        }
        
        public bool CheckEndsWith(string a, string b) {
            if (a.EndsWith(b)) {
                return true;
            }

            return false;
        }

        public bool CheckEndsWithWithOrdinalIgnoreCase(string a, string b) {
            if (a.EndsWith(b, StringComparison.OrdinalIgnoreCase)) {
                return true;
            }

            return false;
        }
    }
}