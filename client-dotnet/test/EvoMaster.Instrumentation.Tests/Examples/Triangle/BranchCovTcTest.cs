using System.Collections.Generic;
using EvoMaster.Instrumentation_Shared;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle {
    public class BranchCovTcTest : CovTcTestBase {
        [Fact]
        public void TestAllBranchesGettingRegistered() {
            const string className = "TriangleClassificationImpl";

            var expectedBranchTargets = new List<string>();
            for (var i = 0; i < 3; i++) {
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 6, i, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 6, i, false));
            }

            for (var i = 0; i < 2; i++) {
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 10, i, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 10, i, false));
            }

            for (var i = 0; i < 6; i++) {
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 16, i, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 16, i, false));
            }

            for (var i = 0; i < 3; i++) {
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 22, i, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 22, i, false));
            }

            var actualBranchTargets = GetRegisteredTargets().Branches;

            Assert.Equal(expectedBranchTargets, actualBranchTargets);
        }
    }
}