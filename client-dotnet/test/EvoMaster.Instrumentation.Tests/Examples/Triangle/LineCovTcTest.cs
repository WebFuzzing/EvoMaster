using System.Collections.Generic;
using System.Linq;
using EvoMaster.Instrumentation.Examples.Branches;
using EvoMaster.Instrumentation.Examples.Numbers;
using EvoMaster.Instrumentation.Examples.Objects;
using EvoMaster.Instrumentation.Examples.Strings;
using EvoMaster.Instrumentation.Examples.Triangle;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;
using Xunit;
using Xunit.Abstractions;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle {
    [Collection("Sequential")]
    public class LineCovTcTest : CovTcTestBase {
        private readonly ITestOutputHelper _testOutputHelper;

        public LineCovTcTest(ITestOutputHelper testOutputHelper) {
            _testOutputHelper = testOutputHelper;
            ExecutionTracer.Reset();
            ObjectiveRecorder.Reset(false); //TODO
        }

        [Fact]
        public void TestLineCoverage() {
            ITriangleClassification tc = new TriangleClassificationImpl();
            ExecutionTracer.Reset();
            ObjectiveRecorder.Reset(false);
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
            _testOutputHelper.WriteLine("Test " + a);
            ITriangleClassification tc = new TriangleClassificationImpl();
            ExecutionTracer.Reset();
            ObjectiveRecorder.Reset(false);
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());

            tc.Classify(a, b, c);

            // Assert.Contains(returnLine, ObjectiveRecorder.AllTargets);
            // here is to check if the specified line is covered, ie, fitness value is 1.0
            Assert.Equal(1.0, ExecutionTracer.GetValue(returnLine));
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
            ObjectiveRecorder.Reset(false);

            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());

            tc.Classify(a, b, c);

            //assert that the last line of the method is reached

            Assert.Equal(1.0, ExecutionTracer.GetValue("Line_at_TriangleClassificationImpl_00027"));
        }


        [Fact]
        public void TestAllTargetsGettingRegistered() {
            ITriangleClassification tc = new TriangleClassificationImpl();

            ExecutionTracer.Reset();
            ObjectiveRecorder.Reset(false);

            tc.Classify(3, 4, 5);

            var expectedLineNumbers = new List<int> {
                5, 6, 7, 10, 11, 14, 16, 18, 19, 22, 23, 26, 27
            };

            var expectedLines = new List<string>();
            expectedLineNumbers.ForEach(x =>
                expectedLines.Add(ObjectiveNaming.LineObjectiveName("TriangleClassificationImpl", x)));


            Assert.True(!expectedLines.Except(ObjectiveRecorder.AllTargets).Any());
            Assert.Contains(ObjectiveNaming.ClassObjectiveName("TriangleClassificationImpl"),
                ObjectiveRecorder.AllTargets);
        }

        [Fact]
        public void TestAllLinesGettingRegistered() {
            var expectedLineNumbers = new List<int> {
                5, 6, 7, 10, 11, 14, 16, 18, 19, 22, 23, 26, 27
            };

            var expectedLines = new List<string>();
            expectedLineNumbers.ForEach(x =>
                expectedLines.Add(ObjectiveNaming.LineObjectiveName("TriangleClassificationImpl", x)));

            var targets = GetRegisteredTargets();

            Assert.Equal(expectedLines, targets.Lines.Where(x => x.Contains("TriangleClassification")));
        }

        [Fact]
        public void TestClassesGettingRegistered() {
            var expectedClassNames = new List<string> {
                nameof(TriangleClassificationImpl),
                nameof(BranchesImp),
                nameof(NumericOperations),
                nameof(StringOperations),
                nameof(ObjectOperations),
                nameof(Student),
                nameof(Instrumentation.Examples.Program)
            };

            var expectedClasses = new List<string>();

            expectedClassNames.ForEach(x => expectedClasses.Add(ObjectiveNaming.ClassObjectiveName(x)));

            var targets = GetRegisteredTargets();
            expectedClasses.ForEach(x => Assert.True(targets.Classes.Contains(x)));
        }

        //This test is added in order to make sure everything works as before after the instrumentation
        [Theory]
        [InlineData(-1, 0, 0, Classification.NOT_A_TRIANGLE)]
        [InlineData(10, 11, 1, Classification.NOT_A_TRIANGLE)]
        [InlineData(6, 6, 6, Classification.EQUILATERAL)]
        [InlineData(7, 6, 7, Classification.ISOSCELES)]
        [InlineData(7, 6, 5, Classification.SCALENE)]
        public void TestFunctionality(int a, int b, int c, Classification expectedOutcome) {
            ITriangleClassification tc = new TriangleClassificationImpl();

            var res = tc.Classify(a, b, c);

            Assert.Equal(expectedOutcome, res);
        }
    }
}