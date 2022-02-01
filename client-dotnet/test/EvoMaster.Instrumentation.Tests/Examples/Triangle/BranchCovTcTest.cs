using System;
using System.Collections.Generic;
using System.Linq;
using EvoMaster.Instrumentation_Shared;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle {
    [Collection("Sequential")]
    public class BranchCovTcTest : CovTcTestBase {
        [Fact]
        public void TestAllBranchesGettingRegistered() {
            const string className = "TriangleClassificationImpl";

            var opCode = string.Empty;
            int lineNo;

            var expectedBranchTargets = new List<string>();
            for (var i = 0; i < 5; i++) {
                lineNo = 6;
                
                switch (i) {
                    case 0:
                    case 1:
                        opCode = "ble";
                        break;
                    case 2:
                        opCode = "cgt";
                        break;
                    case 3:
                        opCode = "ceq";
                        break;
                    case 4:
                        opCode = "brfalse";
                        break;
                }

                RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
            }

            for (var i = 0; i < 3; i++) {
                lineNo = 10;
                
                switch (i) {
                    case 0:
                        opCode = "bne.un";
                        break;
                    case 1:
                        opCode = "ceq";
                        break;
                    case 2:
                        opCode = "brfalse";
                        break;
                }

                RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
            }

            for (var i = 0; i < 8; i++) {
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
                    case 7:
                        opCode = "brfalse";
                        break;
                }

                RegisterBothBranchTargets(expectedBranchTargets, className, lineNo, i, opCode);
            }

            for (var i = 0; i < 4; i++) {
                lineNo = 22;
                
                switch (i) {
                    case 0:
                    case 1:
                        opCode = "beq";
                        break;
                    case 2:
                        opCode = "ceq";
                        break;
                    case 3:
                        opCode = "brfalse";
                        break;
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