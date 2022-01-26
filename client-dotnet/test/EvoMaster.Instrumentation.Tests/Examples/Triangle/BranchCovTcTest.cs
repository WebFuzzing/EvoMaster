using System.Collections.Generic;
using System.Linq;
using EvoMaster.Instrumentation_Shared;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle {
    public class BranchCovTcTest : CovTcTestBase {
        [Fact]
        public void TestAllBranchesGettingRegistered() {
            const string className = "TriangleClassificationImpl";

            string opCode;
            var expectedBranchTargets = new List<string>();
            for (var i = 0; i < 4; i++) {
                opCode = "ldarg";
                if (i == 3)
                    opCode = "call";
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 6, i, opCode, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 6, i, opCode, false));
            }

            for (var i = 0; i < 2; i++) {
                opCode = "ldarg";
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 10, i, opCode, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 10, i, opCode, false));
            }

            for (var i = 0; i < 7; i++) {
                if (i % 2 == 0 && i < 6)
                    opCode = "ldloc";
                else if (i % 2 == 1)
                    opCode = "sub";
                else opCode = "call";

                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 16, i, opCode, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 16, i, opCode, false));
            }

            for (var i = 0; i < 3; i++) {
                opCode = "ldarg";
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 22, i, opCode, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 22, i, opCode, false));
            }

            var actualBranchTargets = GetRegisteredTargets().Branches.Where(x => x.Contains("TriangleClassification"));

            Assert.Equal(expectedBranchTargets, actualBranchTargets);
        }
    }
}