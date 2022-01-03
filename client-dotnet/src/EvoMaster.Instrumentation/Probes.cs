using System;
using System.IO;
using System.Reflection;
using EvoMaster.Client.Util;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;

namespace EvoMaster.Instrumentation {
    public class Probes{

        static Probes(){
            var targets = InitializeTargets();
            foreach (var targetsClass in targets.Classes){
                UnitsInfoRecorder.MarkNewUnit(targetsClass);
                ObjectiveRecorder.RegisterTarget(targetsClass);
            }
            
            foreach (var targetsLine in targets.Lines){
                UnitsInfoRecorder.MarkNewLine();
                ObjectiveRecorder.RegisterTarget(targetsLine);
            }
            
            foreach (var targetsBranch in targets.Branches){
                UnitsInfoRecorder.MarkNewBranch();
                ObjectiveRecorder.RegisterTarget(targetsBranch);
            }
        }
        
        /**
         * With Dotnet, units info are collected based on json file,
         * then require to initialize them based on it
         */
        public static RegisteredTargets InitializeTargets(){
            
            var bin = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);

            if (bin == null) throw new Exception("Executing directory not found");

            var json = File.ReadAllText(Path.Combine(bin, "Targets.json"));

            return Newtonsoft.Json.JsonConvert.DeserializeObject<RegisteredTargets>(json);

        }


        //This method is called by the probe inserted after each covered statement in the instrumented SUT
        public static void CompletedStatement(string className, int lineNo, int columnNo) {
            SimpleLogger.Info($"*** completed {className}: {lineNo}, {columnNo}");
            ExecutionTracer.CompletedStatement(className, lineNo, columnNo);
        }

        public static void EnteringStatement(string className, int lineNo, int columnNo) {
            SimpleLogger.Info($"### entering {className}: {lineNo}, {columnNo}");
            ExecutionTracer.EnteringStatement(className, lineNo, columnNo);
        }
    }
}