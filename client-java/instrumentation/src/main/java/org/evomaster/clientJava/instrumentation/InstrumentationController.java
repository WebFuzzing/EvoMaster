package org.evomaster.clientJava.instrumentation;

import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class InstrumentationController {

    public static void resetForNewSearch(){
        ExecutionTracer.reset();
        ObjectiveRecorder.reset(false);
    }

    /*
        Each time we start/stop/reset the SUT, we need to make sure
        to reset the collection of bytecode info.
     */
    public static void resetForNewTest(){
        ExecutionTracer.reset();

          /*
             Note: it should be fine but, if for any reason EM did not do
             a GET on the targets, then all those newly encountered targets
             would be lost, as EM will have no way to ask for them later, unless
             we explicitly say to return ALL targets
           */
        ObjectiveRecorder.clearFirstTimeEncountered();
    }

    public static void newAction(int actionIndex){

        ExecutionTracer.setActionIndex(actionIndex);
    }

    public static List<TargetInfo> getTargetInfos(Collection<Integer> ids){

        List<TargetInfo> list = new ArrayList<>();

        Map<String, TargetInfo> objectives = ExecutionTracer.getInternalReferenceToObjectiveCoverage();

        ids.stream().forEach(id -> {

            String descriptiveId = ObjectiveRecorder.getDescriptiveId(id);

            TargetInfo info = objectives.get(descriptiveId);
            if(info == null){
                info = TargetInfo.notReached(id);
            } else {
                info = info.withMappedId(id).withNoDescriptiveId();
            }

            list.add(info);
        });

        /*
         *  If new targets were found, we add them even if not requested by EM
         */
        ObjectiveRecorder.getTargetsSeenFirstTime().stream().forEach(s -> {

            int mappedId = ObjectiveRecorder.getMappedId(s);

            TargetInfo info = objectives.get(s).withMappedId(mappedId);

            list.add(info);
        });

        return list;
    }

}
