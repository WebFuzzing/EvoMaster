using EvoMaster.Instrumentation.Examples.Triangle;
using EvoMaster.Instrumentation.StaticState;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle {
    public class LineCovTcTest {
        [Fact]
        public void TestLineCoverage() {
            ITriangleClassification tc = new TriangleClassificationImpl();

            ExecutionTracer.Reset();

            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());

            tc.Classify(-1, 0, 0);

            var a = ExecutionTracer.GetNumberOfObjectives();
            //at least one line should have been covered
            Assert.True(a > 0);
        }

        [Theory]
        [InlineData(-1, 0, 0, "Line_at_TriangleClassificationImpl_00007")]
        [InlineData(6, 6, 6, "Line_at_TriangleClassificationImpl_00011")]
        [InlineData(10, 6, 3, "Line_at_TriangleClassificationImpl_00019")]
        [InlineData(7, 6, 7, "Line_at_TriangleClassificationImpl_00023")]
        [InlineData(7, 6, 5, "Line_at_TriangleClassificationImpl_00026")]
        public void TestSpecificLineCoverage(int a, int b, int c, string returnLine) {
            ITriangleClassification tc = new TriangleClassificationImpl();

            ExecutionTracer.Reset();
            ObjectiveRecorder.Reset(true);

            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            Assert.Empty(ObjectiveRecorder.AllTargets);

            tc.Classify(a, b, c);

            Assert.Contains(returnLine, ObjectiveRecorder.AllTargets);
        }

        [Theory]
        [InlineData(-1, 0, 0)]
        [InlineData(6, 6, 6)]
        [InlineData(10, 6, 3)]
        [InlineData(7, 6, 7)]
        [InlineData(7, 6, 5)]
        public void TestLastLineCoverage(int a, int b, int c) {
            ITriangleClassification tc = new TriangleClassificationImpl();

            ExecutionTracer.Reset();
            ObjectiveRecorder.Reset(true);

            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            Assert.Empty(ObjectiveRecorder.AllTargets);

            tc.Classify(a, b, c);

            //assert that the last line of the method is reached
            Assert.Contains("Line_at_TriangleClassificationImpl_00027", ObjectiveRecorder.AllTargets);
        }
    }
}