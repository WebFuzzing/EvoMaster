using System;
using System.IO;
using System.Reflection;
using EvoMaster.Client.Util;
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
        public static void CompletedStatement(string className, int lineNo, int columnNo) {
            ExecutionTracer.CompletedStatement(className, lineNo, columnNo);
        }

        public static void EnteringStatement(string className, int lineNo, int columnNo) {
            ExecutionTracer.EnteringStatement(className, lineNo, columnNo);
        }

        public static void EnteringBranch(string className, int lineNo, int branchId) {
            //TODO: remove
            SimpleLogger.Info($"****** branch detected at {className}: {lineNo}, {branchId}");
        }

        public static int CompareAndComputeDistance(int val1, int val2, string originalOpCode, string newOpCode, string className, int lineNo, int branchId) {
            Console.WriteLine($"{originalOpCode}: {val1} & {val2}"); //todo

            switch (newOpCode.ToLower()) {
                case "ceq":
                    return val1 == val2 ? 1 : 0;
                case "clt":
                    return val1 < val2 ? 1 : 0;
                case "clt_un":
                    return val1 < val2 ? 1 : 0;
                case "cgt":
                    return val1 > val2 ? 1 : 0;
                case "cgt_un":
                    return val1 > val2 ? 1 : 0;
            }

            throw new Exception($"No match found for the opcode=\"{newOpCode}\"");
        }
        
        public static void ComputeDistanceForOneArgJumps(int val, string opCode, string className, int lineNo, int branchId) {
            Console.WriteLine($"{opCode}: {val}"); //todo
        }
    }
}