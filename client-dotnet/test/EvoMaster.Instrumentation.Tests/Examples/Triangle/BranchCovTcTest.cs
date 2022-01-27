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
            int lineNo;

            var expectedBranchTargets = new List<string>();
            for (var i = 0; i < 4; i++) {
                lineNo = 6;
                opCode = "ble";
                if (i > 1 && i < 3)
                    opCode = "cgt";
                else if (i > 2)
                    opCode = "ceq";

                RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);

                if (i == 3) {
                    opCode = "brfalse";
                    RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
                }
            }

            for (var i = 0; i < 2; i++) {
                lineNo = 10;
                opCode = "bne.un";
                if (i == 1) {
                    opCode = "brfalse";
                    RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
                    opCode = "ceq";
                }

                RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
            }

            for (var i = 0; i < 7; i++) {
                lineNo = 16;
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

                RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);

                if (i == 6) {
                    opCode = "brfalse";
                    RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
                }
            }

            for (var i = 0; i < 3; i++) {
                lineNo = 22;
                opCode = "beq";
                if (i == 2) {
                    opCode = "brfalse";
                    RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
                    opCode = "ceq";
                }

                RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
            }

            expectedBranchTargets = expectedBranchTargets.OrderBy(x => x).ToList();
            var actualBranchTargets = GetRegisteredTargets().Branches.Where(x => x.Contains("TriangleClassification")).OrderBy(x=>x).ToList();

            Assert.Equal(expectedBranchTargets, actualBranchTargets);
        }

        private void RegisterBothBranchTargets(ICollection<string> expectedBranchTargets, string className, int lineNo,
            int i,
            string opCode) {
            expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, lineNo, i, opCode, true));
            expectedBranchTargets.Add(ObjectiveNaming.BranchObjectiveName(className, lineNo, i, opCode, false));
        }
    }
}