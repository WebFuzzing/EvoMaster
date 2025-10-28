using System.Collections.Generic;
using System.Linq;
using EvoMaster.Instrumentation.StaticState;

namespace EvoMaster.Instrumentation {
    public static class InstrumentationController {
        public static void ResetForNewSearch() {
            ExecutionTracer.Reset();
            ObjectiveRecorder.Reset(false);
        }

        /*
            Each time we start/stop/reset the SUT, we need to make sure
            to reset the collection of bytecode info.
         */
        public static void ResetForNewTest() {
            ExecutionTracer.Reset();

            /*
               Note: it should be fine but, if for any reason EM did not do
               a GET on the targets, then all those newly encountered targets
               would be lost, as EM will have no way to ask for them later, unless
               we explicitly say to return ALL targets
             */
            ObjectiveRecorder.ClearFirstTimeEncountered();
        }

        public static List<TargetInfo> GetTargetInfos(IEnumerable<int> ids) {
            var list = new List<TargetInfo>();

            var objectives = ExecutionTracer.GetInternalReferenceToObjectiveCoverage();

            ids.ToList().ForEach(id => {
                var descriptiveId = ObjectiveRecorder.GetDescriptiveId(id);

                var has = objectives.TryGetValue(descriptiveId, out var info);

                info = (info == null || !has) ? TargetInfo.NotReached(id) : info.WithMappedId(id).WithNoDescriptiveId();

                list.Add(info);
            });

            //If new targets were found, we add them even if not requested by EM
            ObjectiveRecorder.GetTargetsSeenFirstTime().ToList().ForEach(s => {
                var mappedId = ObjectiveRecorder.GetMappedId(s);

                var info = objectives[s].WithMappedId(mappedId);

                list.Add(info);
            });

            return list;
        }

        public static List<AdditionalInfo> GetAdditionalInfoList() {
            return new List<AdditionalInfo>(ExecutionTracer.ExposeAdditionalInfoList());
        }
    }
}