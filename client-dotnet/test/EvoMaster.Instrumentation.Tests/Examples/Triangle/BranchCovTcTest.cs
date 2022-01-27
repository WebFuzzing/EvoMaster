using System;
using System.Collections.Generic;
using System.Linq;
using EvoMaster.Instrumentation_Shared;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle {
    public class BranchCovTcTest : CovTcTestBase {
        [Fact]
        public void TestAllBranchesGettingRegistered() {
            const string className = "TriangleClassificationImpl";

            var opCode = string.Empty;
            var expectedBranchTargets = new List<string>();
            for (var i = 0; i < 4; i++) {
                opCode = "ble";
                if (i > 1 && i < 3)
                    opCode = "cgt";
                else if (i > 2)
                    opCode = "ceq";
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 6, i, opCode, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 6, i, opCode, false));
            }

            for (var i = 0; i < 2; i++) {
                opCode = "bne.un";
                if (i == 1)
                    opCode = "ceq";
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 10, i, opCode, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 10, i, opCode, false));
            }

            for (var i = 0; i < 7; i++) {
                switch (i) {
                    case 0:
                    case 2:
                    case 4:
                        opCode = "bne.un";
                        break;
                    case 1:
                    case 3:
                        opCode = "bge";
                        break;
                    case 5:
                        opCode = "clt";
                        break;
                    case 6:
                        opCode = "ceq";
                        break;
                }

                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 16, i, opCode, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 16, i, opCode, false));
            }

            for (var i = 0; i < 3; i++) {
                opCode = "beq";
                if (i == 2)
                    opCode = "ceq";

                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 22, i, opCode, true));
                expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, 22, i, opCode, false));
            }

            var actualBranchTargets = GetRegisteredTargets().Branches.Where(x => x.Contains("TriangleClassification"));

            Assert.Equal(expectedBranchTargets, actualBranchTargets);
        }
    }
}