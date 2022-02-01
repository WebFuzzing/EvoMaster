using System;
using System.IO;
using System.Reflection;
using EvoMaster.Client.Util;
using EvoMaster.Instrumentation.Coverage.MethodReplacement;
using EvoMaster.Instrumentation.Heuristic;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation {
    public class Probes {
        static Probes() {
            InitializeTargets();
            SimpleLogger.Info("All targets are registered.");
        }

        /// <summary>
        ///   With Dotnet, units info are collected based on json file,then require to initialize them based on it
        /// </summary>
        /// <exception cref="DirectoryNotFoundException">In case the executing directory (bin folder) couldn't be found</exception>
        private static void InitializeTargets() {
            var bin = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);

            if (bin == null) throw new DirectoryNotFoundException("Executing directory not found");

            var json = File.ReadAllText(Path.Combine(bin, "Targets.json"));

            var targets = Newtonsoft.Json.JsonConvert.DeserializeObject<RegisteredTargets>(json);

            foreach (var targetsClass in targets.Classes) {
                UnitsInfoRecorder.MarkNewUnit(targetsClass);
                ObjectiveRecorder.RegisterTarget(targetsClass);
            }

            foreach (var targetsLine in targets.Lines) {
                UnitsInfoRecorder.MarkNewLine();
                ObjectiveRecorder.RegisterTarget(targetsLine);
            }

            foreach (var targetsBranch in targets.Branches) {
                UnitsInfoRecorder.MarkNewBranch();
                ObjectiveRecorder.RegisterTarget(targetsBranch);
            }

            //TODO for statement if needed
        }


        //This method is called by the probe inserted after each covered statement in the instrumented SUT
        public static void CompletedStatement(string className, string method, int lineNo, int columnNo) {
            ExecutionTracer.CompletedStatement(className, method, lineNo, columnNo);
        }

        public static void EnteringStatement(string className, string method, int lineNo, int columnNo) {
            ExecutionTracer.EnteringStatement(className, method, lineNo, columnNo);
        }

        //This method (and its other overloadings below) imitate comparison instructions.
        //They do exactly the same, in addition to computing distance
        //We had to do this because we couldn't find a way to duplicate two top values on the evalutation stack which was needed to compute the distance
        public static int CompareAndComputeDistance(int val1, int val2,
            string newOpCode, string className, int lineNo, int branchId) {
            HeuristicsForBooleans.ComputeDistanceForTwoArgs(className, lineNo, branchId, val1, val2, newOpCode);

            switch (newOpCode.ToLower()) {
                case "ceq":
                    return val1.Equals(val2) ? 1 : 0;

                case "clt":
                    return val1 < val2 ? 1 : 0;
                case "clt.un":
                    return (uint) val1 < (uint) val2 ? 1 : 0;

                case "cgt":
                    return val1 > val2 ? 1 : 0;
                case "cgt.un":
                    return (uint) val1 > (uint) val2 ? 1 : 0;
            }

            throw new Exception($"No match found for the opcode=\"{newOpCode}\"");
        }

        //TODO: check clt.un and cgt.un
        public static int CompareAndComputeDistance(double val1, double val2,
            string newOpCode, string className, int lineNo, int branchId) {
            HeuristicsForBooleans.ComputeDistanceForTwoArgs(className, lineNo, branchId, val1, val2, newOpCode);

            newOpCode = newOpCode.ToLower();

            if (double.IsNaN(val1) || double.IsNaN(val2)) {
                switch (newOpCode) {
                    case "cgt.un":
                    case "clt.un":
                        return 1;
                    case "ceq":
                    case "clt":
                    case "cgt":
                        return 0;
                }
            }

            switch (newOpCode) {
                case "ceq":
                    return val1.Equals(val2) ? 1 : 0;

                case "clt":
                case "clt.un":
                    return val1 < val2 ? 1 : 0;
                case "cgt":
                case "cgt.un":
                    return val1 > val2 ? 1 : 0;
            }

            throw new Exception($"No match found for the opcode=\"{newOpCode}\"");
        }

        //TODO: check clt.un and cgt.un
        public static int CompareAndComputeDistance(float val1, float val2,
            string newOpCode, string className, int lineNo, int branchId) {
            HeuristicsForBooleans.ComputeDistanceForTwoArgs(className, lineNo, branchId, val1, val2, newOpCode);

            if (double.IsNaN(val1) || double.IsNaN(val2)) {
                switch (newOpCode) {
                    case "cgt.un":
                    case "clt.un":
                        return 1;
                    case "ceq":
                    case "clt":
                    case "cgt":
                        return 0;
                }
            }

            switch (newOpCode.ToLower()) {
                case "ceq":
                    return val1.Equals(val2) ? 1 : 0;

                case "clt":
                case "clt.un":
                    return val1 < val2 ? 1 : 0;

                case "cgt":
                case "cgt.un":
                    return val1 > val2 ? 1 : 0;
            }

            throw new Exception($"No match found for the opcode=\"{newOpCode}\"");
        }

        public static int CompareAndComputeDistance(long val1, long val2,
            string newOpCode, string className, int lineNo, int branchId) {
            HeuristicsForBooleans.ComputeDistanceForTwoArgs(className, lineNo, branchId, val1, val2, newOpCode);

            switch (newOpCode.ToLower()) {
                case "ceq":
                    return val1.Equals(val2) ? 1 : 0;

                case "clt":
                    return (ulong) val1 < (ulong) val2 ? 1 : 0;
                case "clt.un":
                    return val1 < val2 ? 1 : 0;

                case "cgt":
                    return val1 > val2 ? 1 : 0;
                case "cgt.un":
                    return (ulong) val1 > (ulong) val2 ? 1 : 0;
            }

            throw new Exception($"No match found for the opcode=\"{newOpCode}\"");
        }

        public static int CompareAndComputeDistance(short val1, short val2,
            string newOpCode, string className, int lineNo, int branchId) {
            HeuristicsForBooleans.ComputeDistanceForTwoArgs(className, lineNo, branchId, val1, val2, newOpCode);

            switch (newOpCode.ToLower()) {
                case "ceq":
                    return val1.Equals(val2) ? 1 : 0;

                case "clt":
                    return (ushort) val1 < (ushort) val2 ? 1 : 0;
                case "clt.un":
                    return val1 < val2 ? 1 : 0;

                case "cgt":
                    return val1 > val2 ? 1 : 0;
                case "cgt.un":
                    return (ushort) val1 > (ushort) val2 ? 1 : 0;
            }

            throw new Exception($"No match found for the opcode=\"{newOpCode}\"");
        }

        //This method computes the distance for the value before a jump instruction with one argument (brtrue & brfalse)
        //We didn't have to imitate the behaviour of the instruction as it was taking one argument(unlike the methods
        //above which are aimed for instructions which take two arguments) and we could just add a dup instruction to duplicate
        //the top value on the evaluation stack (this is done in the Instrumentator.cs)
        public static void ComputeDistanceForOneArgJumps(int val, string opCode, string className, int lineNo,
            int branchId) {
            HeuristicsForBooleans.ComputeDistanceForSingleJump(className, lineNo, branchId, val, opCode);
        }

        public static void ComputeDistanceForOneArgJumps(double val, string opCode, string className, int lineNo,
            int branchId) {
            // add warning just in case
            SimpleLogger.Warn("in theory, opCode should not work with float but found it at " + lineNo + " in" +
                              className);
        }

        public static void ComputeDistanceForOneArgJumps(float val, string opCode, string className, int lineNo,
            int branchId) {
            // add warning just in case
            SimpleLogger.Warn("in theory, opCode should not work with float but found it at " + lineNo + " in" +
                              className);
        }

        public static void ComputeDistanceForOneArgJumps(long val, string opCode, string className, int lineNo,
            int branchId) {
            HeuristicsForBooleans.ComputeDistanceForSingleJump(className, lineNo, branchId, val, opCode);
        }

        public static void ComputeDistanceForOneArgJumps(short val, string opCode, string className, int lineNo,
            int branchId) {
            HeuristicsForBooleans.ComputeDistanceForSingleJump(className, lineNo, branchId, val, opCode);
        }

        public static bool ObjectEquality(object val1, object val2, string className, int lineNo, int branchId) {
            var templateId = ObjectiveNaming.MethodReplacementObjectiveNameTemplate(className, lineNo, branchId);
            if (val1!= null && val1 is string sval1){
                // we only compute distance for string.equals(object)
                // https://docs.microsoft.com/en-us/dotnet/api/system.string.equals?view=netcore-3.1#system-string-equals(system-object)
                return StringClassReplacement.EqualsObject(sval1, val2, templateId);
            } else if (val1 != null){
                return ObjectClassReplacement.EqualsObject(val1, val2, templateId);
            }
            return val1.Equals(val2);
        }
        
        public static bool StringEquality(string val1, string val2, string className, int lineNo, int branchId) {
            
            var templateId = ObjectiveNaming.MethodReplacementObjectiveNameTemplate(className, lineNo, branchId);
            return StringClassReplacement.Equals(val1, val2, templateId);
            //return val1 == val2;
        }

        public static bool StringCompareWithComparison(string val1, string val2, int comparison, string operand, string className,
            int lineNo, int branchId) {
            
            StringComparison c;
            try {
                c = (StringComparison) comparison;
            }
            catch (Exception e) {
                throw new Exception($"Not able to convert {comparison} to a StringComparison enum");
            }

            var templateId = ObjectiveNaming.MethodReplacementObjectiveNameTemplate(className, lineNo, branchId);

            if (operand.Contains("Equals"))
                return StringClassReplacement.Equals(val1, val2, c, templateId);
                //return val1.Equals(val2, c);
            if (operand.Contains("Contains"))
                return StringClassReplacement.Contains(val1, val2, c, templateId);
                //return val1.Contains(val2, c);
            if (operand.Contains("StartsWith"))
                return StringClassReplacement.StartsWith(val1, val2, c, templateId);
                //return val1.StartsWith(val2, c);
            if (operand.Contains("EndsWith"))
                return StringClassReplacement.EndsWith(val1, val2, c, templateId);
                // return val1.EndsWith(val2, c);

            throw new Exception($"No string method found for {operand}");
        }

        public static bool StringCompare(string val1, string val2, string operand, string className, int lineNo,
            int branchId) {
            
            var templateId = ObjectiveNaming.MethodReplacementObjectiveNameTemplate(className, lineNo, branchId);

            if (operand.Contains("Equals"))
                return StringClassReplacement.Equals(val1, val2, templateId);
                //return val1.Equals(val2);
            if (operand.Contains("Contains"))
                return StringClassReplacement.Contains(val1, val2, templateId);
                //return val1.Contains(val2);
            if (operand.Contains("StartsWith"))
                return StringClassReplacement.StartsWith(val1, val2, templateId);
                //return val1.StartsWith(val2);
            if (operand.Contains("EndsWith"))
                return StringClassReplacement.EndsWith(val1, val2, templateId);
                //return val1.EndsWith(val2);

            throw new Exception($"No string method found for {operand}");
        }
    }
}