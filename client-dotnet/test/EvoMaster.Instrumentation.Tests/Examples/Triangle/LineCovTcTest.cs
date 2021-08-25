using EvoMaster.Instrumentation.Examples.Triangle;
using EvoMaster.Instrumentation.StaticState;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle
{
    public class LineCovTcTest
    {
        [Fact]
        public void TestLineCov()
        {
            ITriangleClassification tc = new TriangleClassificationImpl();

            ExecutionTracer.Reset(); 
           
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());

            tc.Classify(-1, 0, 0);
            
            var a = ExecutionTracer.GetNumberOfObjectives();
            //at least one line should had been covered
            Assert.True(a > 0);
        }
    }
}