using EvoMaster.Client.Util;
using EvoMaster.Instrumentation.StaticState;

namespace EvoMaster.Instrumentation {
    public class Probes {
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