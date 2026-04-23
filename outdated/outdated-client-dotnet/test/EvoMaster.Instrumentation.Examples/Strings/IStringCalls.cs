using System;

namespace EvoMaster.Instrumentation.Examples.strings{
    public interface IStringCalls{
        
        bool callEquals(string a, object b);

        bool callEqualsStringComparison(string a, string b, StringComparison comparison);

        bool callStartsWith(string a, string b);
        
        bool callStartsWithStringComparison(string a, string b, StringComparison comparison);

        // does not exist in dotnet
        // bool callStartsWith(string a, string b, int toffset);

        bool callEndsWith(string a, string b);
        
        bool callEndsWithStringComparison(string a, string b, StringComparison comparison);

        
        // does not exist in dotnet
        // bool callIsEmpty(string a);
        // boolean callContentEquals(String a, CharSequence cs);
        // boolean callContentEquals(String a, StringBuffer sb);

        bool callContains(string a, string cs);
        
        bool callContainsStringComparison(string a, string cs, StringComparison comparison);
    }
}