using System.Collections.Generic;
using EvoMaster.Instrumentation.Examples.Branches;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Branches{
    [Collection("Sequential")]
    public class BranchesInstrumentedTest{
        
        [Fact]
        public void TestPosX(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res = bs.Pos(10, 0);
            //first branch should had been taken
            Assert.Equal(0, res);

            //so far, seen only first "if", of which the else is not covered
            //for dotnet, there are more targets
            Assert.Equal(4, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(2, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));

            ISet<string> notCoveredBranch = ExecutionTracer.GetNonCoveredObjectives(ObjectiveNaming.Branch);
            string elseBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 4, 0, "cgt", false);
            Assert.True(notCoveredBranch.Contains(elseBranch));

            double first = ExecutionTracer.GetValue(elseBranch);
            Assert.True(first < 1d); // not covered

            bs.Pos(15, 0); //worse value, should have no impact
            double second = ExecutionTracer.GetValue(elseBranch);
            Assert.True(second < 1d); // still not covered
            Assert.Equal(first, second);

            bs.Pos(8, 0); //better value
            double third = ExecutionTracer.GetValue(elseBranch);
            Assert.True(third < 1d); // still not covered
            Assert.True(third > first);
        }
        
        [Fact]
        public void TestPosXDouble(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res = bs.PosDouble(10.0, 0.0);
            //first branch should had been taken
            Assert.Equal(9, res);

            //so far, seen only first "if", of which the else is not covered
            //for dotnet, there are more targets
            Assert.Equal(4, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(2, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));

            ISet<string> notCoveredBranch = ExecutionTracer.GetNonCoveredObjectives(ObjectiveNaming.Branch);
            string elseBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 40, 0, "cgt", false);
            Assert.True(notCoveredBranch.Contains(elseBranch));

            double first = ExecutionTracer.GetValue(elseBranch);
            Assert.True(first < 1d); // not covered

            bs.PosDouble(15.0, 0.0); //worse value, should have no impact
            double second = ExecutionTracer.GetValue(elseBranch);
            Assert.True(second < 1d); // still not covered
            Assert.Equal(first, second);

            bs.PosDouble(8.0, 0.0); //better value
            double third = ExecutionTracer.GetValue(elseBranch);
            Assert.True(third < 1d); // still not covered
            Assert.True(third > first);


            res = bs.PosDouble(0.0 / 0.0, 0.0 / 0.0);
            Assert.Equal(11, res);
            double fourth = ExecutionTracer.GetValue(elseBranch);
            Assert.Equal(1.0, fourth);
            
        }

        [Fact]
        public void TestPosY(){
            
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res;
            res = bs.Pos(10, 0);
            Assert.Equal(0, res);

            res = bs.Pos(-5, 4);
            Assert.Equal(1, res);

            //seen 2 "if", but returned on the second "if"
            //for 1st-if 4 branches (cgt+brfalse) + for 2nd-if 6 branches(clt+ceq+brfalse)
            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(3, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));

            ISet<string> notCovered = ExecutionTracer.GetNonCoveredObjectives(ObjectiveNaming.Branch);
            string cltBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 8, 0, "clt", true);
            string elseCeqBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 8, 1, "ceq", false);
            Assert.True(notCovered.Contains(cltBranch));
            Assert.True(notCovered.Contains(elseCeqBranch));

            double first = ExecutionTracer.GetValue(cltBranch);
            Assert.True(first < 1d); // not covered

            bs.Pos(-8, 8); //worse value, should have no impact
            double second = ExecutionTracer.GetValue(cltBranch);
            Assert.True(second < 1d); // still not covered
            Assert.Equal(first, second);

            bs.Pos(-8, 0); //better value, but still not covered
            double third = ExecutionTracer.GetValue(cltBranch);
            Assert.True(third < 1d); // still not covered
            Assert.True(third > second);

            //all branches covered
            res = bs.Pos(-89, -45);
            Assert.Equal(2, res);

            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(0, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));

            Assert.True(ObjectiveRecorder.ComputeCoverage(ObjectiveNaming.Branch) > 0);
        }
        
         [Fact]
        public void TestPosYDouble(){
            
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res;
            res = bs.PosDouble(10.0, 0.0);
            Assert.Equal(9, res);

            res = bs.PosDouble(-5, 4);
            Assert.Equal(10, res);

            //seen 2 "if", but returned on the second "if"
            //for 1st-if 4 branches (cgt+brfalse) + for 2nd-if 6 branches(clt+ceq+brfalse)
            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(3, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));

            ISet<string> notCovered = ExecutionTracer.GetNonCoveredObjectives(ObjectiveNaming.Branch);
            string cltBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 44, 0, "clt.un", true);
            string elseCeqBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 44, 1, "ceq", false);
            Assert.True(notCovered.Contains(cltBranch));
            Assert.True(notCovered.Contains(elseCeqBranch));

            double first = ExecutionTracer.GetValue(cltBranch);
            Assert.True(first < 1d); // not covered

            bs.PosDouble(-8, 8); //worse value, should have no impact
            double second = ExecutionTracer.GetValue(cltBranch);
            Assert.True(second < 1d); // still not covered
            Assert.Equal(first, second);

            bs.PosDouble(-8, 0); //better value, but still not covered
            double third = ExecutionTracer.GetValue(cltBranch);
            Assert.True(third < 1d); // still not covered
            Assert.True(third > second);

            // else could be covered by NaN
            res = bs.PosDouble(0.0/0.0, 0.0/0.0);
            Assert.Equal(11, res);

            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(0, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));

            res = bs.PosDouble(-89, -45);
            Assert.Equal(11, res);
            
            Assert.True(ObjectiveRecorder.ComputeCoverage(ObjectiveNaming.Branch) > 0);
        }


        [Fact]
        public void TestNegX(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res = bs.Neg(-10, 0);
            //first branch should had been taken
            Assert.Equal(3, res);
        
            //so far, seen only first "if", of which the else is not covered
            Assert.Equal(4, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(2, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
        
            ISet<string> notCoveredBranch = ExecutionTracer.GetNonCoveredObjectives(ObjectiveNaming.Branch);
            string elseBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 16, 0, "clt", false);
            Assert.True(notCoveredBranch.Contains(elseBranch));

            double first = ExecutionTracer.GetValue(elseBranch);
            Assert.True(first < 1d); // not covered
        
        
            bs.Neg(-15, 0); //worse value, should have no impact
            double second = ExecutionTracer.GetValue(elseBranch);
            Assert.True(second < 1d); // still not covered
            Assert.Equal(first, second);
        
            bs.Neg(-8, 0); //better value
            double third = ExecutionTracer.GetValue(elseBranch);
            Assert.True(third < 1d); // still not covered
            Assert.True(third > first);
        }
        
        [Fact]
        public void TestNegXDouble(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res = bs.NegDouble(-10, 0);
            //first branch should had been taken
            Assert.Equal(12, res);
        
            //so far, seen only first "if", of which the else is not covered
            Assert.Equal(4, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(2, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
        
            ISet<string> notCoveredBranch = ExecutionTracer.GetNonCoveredObjectives(ObjectiveNaming.Branch);
            string elseBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 52, 0, "clt", false);
            Assert.True(notCoveredBranch.Contains(elseBranch));

            double first = ExecutionTracer.GetValue(elseBranch);
            Assert.True(first < 1d); // not covered
        
        
            bs.NegDouble(-15, 0); //worse value, should have no impact
            double second = ExecutionTracer.GetValue(elseBranch);
            Assert.True(second < 1d); // still not covered
            Assert.Equal(first, second);
        
            bs.NegDouble(-8, 0); //better value
            double third = ExecutionTracer.GetValue(elseBranch);
            Assert.True(third < 1d); // still not covered
            Assert.True(third > first);
        }
        
        [Fact]
        public void TestNegY(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res;
            res = bs.Neg(-10, 0);
            Assert.Equal(3, res);
        
            res = bs.Neg(5, -4);
            Assert.Equal(4, res);
        
            //seen 2 "if", but returned on the second "if"
            //for 1st-if 4 branches (cgt+brfalse) + for 2nd-if 6 branches(cgt+ceq+brfalse)
            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(3, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
            
            ISet<string> notCovered = ExecutionTracer.GetNonCoveredObjectives(ObjectiveNaming.Branch);
            string cgtBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 20, 0, "cgt", true);
            string elseCeqBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 20, 1, "ceq", false);
            Assert.True(notCovered.Contains(cgtBranch));
            Assert.True(notCovered.Contains(elseCeqBranch));
            
        
            double first = ExecutionTracer.GetValue(cgtBranch);
            Assert.True(first < 1d); // not covered
        
            bs.Neg(8, -8); //worse value, should have no impact
            double second = ExecutionTracer.GetValue(cgtBranch);
            Assert.True(second < 1d); // still not covered
            Assert.Equal(first, second);
        
            bs.Neg(8, -1); //better value, but still not covered
            double third = ExecutionTracer.GetValue(cgtBranch);
            Assert.True(third < 1d); // still not covered
            Assert.True(third > second);
        
            //all branches covered
            res = bs.Neg(89, 45);
            Assert.Equal(5, res);
        
            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(0, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
        }
        
        [Fact]
        public void TestNegYDouble(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res;
            res = bs.NegDouble(-10, 0);
            Assert.Equal(12, res);
        
            res = bs.NegDouble(5, -4);
            Assert.Equal(13, res);
        
            //seen 2 "if", but returned on the second "if"
            //for 1st-if 4 branches (cgt+brfalse) + for 2nd-if 6 branches(cgt+ceq+brfalse)
            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(3, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
            
            ISet<string> notCovered = ExecutionTracer.GetNonCoveredObjectives(ObjectiveNaming.Branch);
            string cgtBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 56, 0, "cgt.un", true);
            string elseCeqBranch = ObjectiveNaming.BranchObjectiveName("BranchesImp", 56, 1, "ceq", false);
            Assert.True(notCovered.Contains(cgtBranch));
            Assert.True(notCovered.Contains(elseCeqBranch));
            
        
            double first = ExecutionTracer.GetValue(cgtBranch);
            Assert.True(first < 1d); // not covered
        
            bs.NegDouble(8, -8); //worse value, should have no impact
            double second = ExecutionTracer.GetValue(cgtBranch);
            Assert.True(second < 1d); // still not covered
            Assert.Equal(first, second);
        
            bs.NegDouble(8, -1); //better value, but still not covered
            double third = ExecutionTracer.GetValue(cgtBranch);
            Assert.True(third < 1d); // still not covered
            Assert.True(third > second);
        
            //all branches covered
            res = bs.NegDouble(89, 45);
            Assert.Equal(14, res);
        
            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(0, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
        }
        
        [Fact]
        public void TestEq(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res;
            res = bs.Eq(0, 0);
            Assert.Equal(6, res);
        
            Assert.Equal(4, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(2, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
        
            res = bs.Eq(2, 5);
            Assert.Equal(7, res);
        
            Assert.Equal(8, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(2, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
        
            res = bs.Eq(2, 0);
            Assert.Equal(8, res);
        
            Assert.Equal(8, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(0, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
            
        }
        
        [Fact]
        public void TestEqDouble(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            int res;
            res = bs.EqDouble(0, 0);
            Assert.Equal(15, res);
        
            Assert.Equal(4, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(2, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
        
            // res = bs.EqDouble(2, 5);
            // Assert.Equal(16, res);
            
            // regarding NaN != 0
            // it is converted into ceq+ceq
            // NAN 0 ceq pushes 0, then 0 0 ceq pushes 1
            res = bs.EqDouble(0.0 / 0.0, 0.0 / 0.0);
            Assert.Equal(16, res);
        
            // note that, regarding ==
            // int is cgt.un with unsigned format
            // double is ceq and ceq
            // thus double has two more branches than int
            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(3, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
            
            
            res = bs.EqDouble(2, 0);
            Assert.Equal(17, res);
        
            Assert.Equal(10, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(0, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
            
            
            
        }

        [Fact]
        public void TestAll(){
            IBranches bs = new BranchesImp();
            
            ObjectiveRecorder.Reset(false);
            ExecutionTracer.Reset();
            Assert.Equal(0, ExecutionTracer.GetNumberOfObjectives());
            
            bs.Pos(1, 1);
            bs.Pos(-1, 1);
            bs.Pos(-1, -1);
            
            bs.PosDouble(1, 1);
            bs.PosDouble(-1, 1);
            bs.PosDouble(-1, -1);

            bs.Neg(-1, -1);
            bs.Neg(1, -1);
            bs.Neg(1, 1);

            bs.NegDouble(-1, -1);
            bs.NegDouble(1, -1);
            bs.NegDouble(1, 1);
            
            bs.Eq(0, 0);
            bs.Eq(4, 0);
            bs.Eq(5, 5);
            
            bs.EqDouble(0, 0);
            bs.EqDouble(4, 0);
            bs.EqDouble(5, 5);

            // 28 int + 30 double (regarding extra two in double, see TestEqDouble)
            Assert.Equal(58, ExecutionTracer.GetNumberOfObjectives(ObjectiveNaming.Branch));
            Assert.Equal(0, ExecutionTracer.GetNumberOfNonCoveredObjectives(ObjectiveNaming.Branch));
        }
    }
    
}