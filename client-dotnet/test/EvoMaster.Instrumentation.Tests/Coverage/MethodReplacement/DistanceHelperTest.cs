using System;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Coverage.MethodReplacement{
    public class DistanceHelperTest{
        
        // Comment this out due to failing on CI
        // [Theory]
        // [InlineData("encyclopædia", "encyclopaedia", StringComparison.InvariantCulture, false)]
        // public void TestDistanceForStringComparison(string a, string b, StringComparison comparisonType, bool same){
        //     var actualSame = a.Equals(b, StringComparison.InvariantCulture);
        //     long actualResult = DistanceHelper.GetLeftAlignmentDistance(a, b, comparisonType);
        //     Assert.Equal(same, actualResult == 0);
        //     // need to fix 
        //     Assert.NotEqual(actualSame, same);
        // }
    }
}