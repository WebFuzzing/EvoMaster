using System;
using System.IO;
using System.Reflection;
using EvoMaster.Client.Util;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;

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
            if (className.Contains("PrivateImplementationDetails")) return;
            //TODO: remove
            SimpleLogger.Info($"****** branch detected at {className}: {lineNo}, {branchId}");
        }
    }
}