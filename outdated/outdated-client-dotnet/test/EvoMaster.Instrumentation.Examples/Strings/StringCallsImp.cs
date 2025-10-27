using System;
using EvoMaster.Instrumentation.Examples.strings;

namespace EvoMaster.Instrumentation.Examples.Strings{
    public class StringCallsImp : IStringCalls{
        public bool callEquals(string a, object b){
            if(a.Equals(b))
                return true;
            return false;
        }

        public bool callEqualsStringComparison(string a, string b, StringComparison comparison){
            if(a.Equals(b, comparison))
                return true;
            return false;
        }

        public bool callStartsWith(string a, string b){
            if(a.StartsWith(b))
                return true;
            return false;
        }

        public bool callStartsWithStringComparison(string a, string b, StringComparison comparison){
            if(a.StartsWith(b, comparison))
                return true;
            return false;
        }

        public bool callEndsWith(string a, string b){
            if(a.EndsWith(b))
                return true;
            return false;
        }

        public bool callEndsWithStringComparison(string a, string b, StringComparison comparison){
            if(a.EndsWith(b, comparison))
                return true;
            return false;
        }

        public bool callContains(string a, string cs){
            if(a.Contains(cs))
                return true;
            return false;
        }

        public bool callContainsStringComparison(string a, string cs, StringComparison comparison){
            if(a.Contains(cs, comparison))
                return true;
            return false;
        }
    }
}