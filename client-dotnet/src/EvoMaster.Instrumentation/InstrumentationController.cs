
using System.Collections.Generic;
using EvoMaster.Instrumentation.Staticstate;

namespace EvoMaster.Instrumentation
{
    public static class InstrumentationController
    {
        public static void ResetForNewSearch(){
            ExecutionTracer.Reset();
            //ObjectiveRecorder.reset(false);
        }

        /*
            Each time we start/stop/reset the SUT, we need to make sure
            to reset the collection of bytecode info.
         */
        public static void ResetForNewTest(){
            ExecutionTracer.Reset();

            /*
               Note: it should be fine but, if for any reason EM did not do
               a GET on the targets, then all those newly encountered targets
               would be lost, as EM will have no way to ask for them later, unless
               we explicitly say to return ALL targets
             */
            //ObjectiveRecorder.clearFirstTimeEncountered();
        }
        
        public static List<TargetInfo> GetTargetInfos(IEnumerable<int> ids){
            
            //return empty
            return new List<TargetInfo>();
            
        }
        
        public static List<AdditionalInfo> GetAdditionalInfoList(){
            return new List<AdditionalInfo>(ExecutionTracer.ExposeAdditionalInfoList());
        }
    }
}