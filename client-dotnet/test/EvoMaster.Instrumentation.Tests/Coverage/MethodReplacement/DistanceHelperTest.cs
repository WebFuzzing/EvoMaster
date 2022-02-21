using System;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Coverage.MethodReplacement{
    public class DistanceHelperTest{
        
        [Theory(Skip = "Failing on CI due to different culture")]
        [InlineData("encyclopædia", "encyclopaedia", StringComparison.InvariantCulture, false)]
        public void TestDistanceForStringComparison(string a, string b, StringComparison comparisonType, bool same){
            var actualSame = a.Equals(b, StringComparison.InvariantCulture);
            long actualResult = DistanceHelper.GetLeftAlignmentDistance(a, b, comparisonType);
            Assert.Equal(same, actualResult == 0);
            // need to fix 
            Assert.NotEqual(actualSame, same);
        }
        
        [Fact]
        public void TestConstants() {
            Assert.True(0 < DistanceHelper.H_REACHED_BUT_NULL);
            Assert.True(DistanceHelper.H_REACHED_BUT_NULL < DistanceHelper.H_NOT_NULL);
            Assert.True(DistanceHelper.H_NOT_NULL < 1);
        }
        
        [Fact]
        public void TestDistanceDigit() {

            Assert.Equal(0, DistanceHelper.DistanceToDigit('0'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('1'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('2'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('3'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('4'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('5'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('6'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('7'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('8'));
            Assert.Equal(0, DistanceHelper.DistanceToDigit('9'));

            //see ascii table
            Assert.Equal(1, DistanceHelper.DistanceToDigit('/'));
            Assert.Equal(2, DistanceHelper.DistanceToDigit('.'));
            Assert.Equal(1, DistanceHelper.DistanceToDigit(':'));
            Assert.Equal(2, DistanceHelper.DistanceToDigit(';'));

            Assert.True(DistanceHelper.DistanceToDigit('a') < DistanceHelper.DistanceToDigit('b'));
        }
        
        [Fact]
        public void TestIntegerDistance() {
            double distance = DistanceHelper.GetDistanceToEquality(-10, 10);
            Assert.Equal(20, distance);
            
                
        }

        [Fact]
        public void TestIntegerMaxDistance() {
            double distance = DistanceHelper.GetDistanceToEquality(int.MinValue, int.MaxValue);
            Assert.Equal(Math.Pow(2, 32) - 1, distance);
        }


        [Fact]
        public void TestLongMaxDistance() {
            double distance = DistanceHelper.GetDistanceToEquality(long.MinValue, long.MaxValue);
            Assert.Equal(Math.Pow(2, 64) - 1, distance);
        }

        [Fact]
        public void TestDoubleOverflowsDistance() {
            double distance = DistanceHelper.GetDistanceToEquality(-double.MaxValue, double.MaxValue);
            Assert.Equal(double.MaxValue, distance);
        }

        [Fact]
        public void TestDoubleMaxDistance() {
            double upperBound = double.MaxValue  /2;
            double lowerBound = -upperBound;
            double distance = DistanceHelper.GetDistanceToEquality(lowerBound, upperBound);
            Assert.Equal(double.MaxValue, distance);
        }

        [Fact]
        public void TestHeuristicFromScaledDistanceWithBase(){
            Assert.Throws<ArgumentException>(() => { DistanceHelper.HeuristicFromScaledDistanceWithBase(-1, 0.0);});
            Assert.Throws<ArgumentException>(() => { DistanceHelper.HeuristicFromScaledDistanceWithBase(2, 0.0);});
            Assert.Throws<ArgumentException>(() => { DistanceHelper.HeuristicFromScaledDistanceWithBase(0.5, -1);});
            
            Assert.Equal(0.5, DistanceHelper.HeuristicFromScaledDistanceWithBase(0.5, double.MaxValue));
            Assert.Equal(0.5, DistanceHelper.HeuristicFromScaledDistanceWithBase(0.5, 1.0/0.0));
            Assert.Equal(0.75, DistanceHelper.HeuristicFromScaledDistanceWithBase(0.5, 1));
        }
    }
}