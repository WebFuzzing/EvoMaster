package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static void newAction(Action action){

        ExecutionTracer.setAction(action);
    }

    public static void setKillSwitch(boolean b){
        ExecutionTracer.setKillSwitch(b);
    }

    public static void setExecutingInitSql(boolean executingInitSql){
        ExecutionTracer.setExecutingInitSql(executingInitSql);
    }

    public static void setExecutingInitMongo(boolean executingInitMongo){
        ExecutionTracer.setExecutingInitMongo(executingInitMongo);
    }

    public static void setExecutingAction(boolean executingAction){
        ExecutionTracer.setExecutingAction(executingAction);
    }

    public static void setBootingSut(boolean bootingSut){
        ObjectiveRecorder.setBooting(bootingSut);
    }

    public static List<TargetInfo> getTargetInfos(
            Collection<Integer> ids,
            boolean fullyCovered,
            boolean descriptiveIds
    ){

        List<TargetInfo> list = new ArrayList<>();

        Map<String, TargetInfo> objectives = ExecutionTracer.getInternalReferenceToObjectiveCoverage();

        if(ids != null) {
            ids.stream().forEach(id -> {

                String descriptiveId = ObjectiveRecorder.getDescriptiveId(id);

                TargetInfo info = objectives.get(descriptiveId);
                if (info == null) {
                    info = TargetInfo.notReached(id);
                } else {
                    info = info.withMappedId(id);
                    if(!descriptiveIds) {
                        info = info.withNoDescriptiveId();
                    }
                }

                list.add(info);
            });

            /*
             *  If new targets were found, we add them even if not requested by EM
             */
            ObjectiveRecorder.getTargetsSeenFirstTime().stream().forEach(s -> {

                int mappedId = ObjectiveRecorder.getMappedId(s);

                TargetInfo info = objectives.get(s).withMappedId(mappedId);
                //always adding here descriptiveId

                list.add(info);
            });

        } else {

            List<String> seenFirstTime = ObjectiveRecorder.getTargetsSeenFirstTime();

            //if specified ids is null, then get all, but not the ones at booting time
            objectives.entrySet()
                    .stream()
                    .filter(e -> !ObjectiveRecorder.wasCollectedAtBootingTime(e.getKey()))
                    //try to save bandwidth by only sending mapped ids
                    .map(e -> {
                        TargetInfo info = e.getValue().enforceMappedId();
                        if(!descriptiveIds &&
                                seenFirstTime.isEmpty()
                                /*
                                    FIXME following check would be more correct, but leads to failures.
                                    Look like some targets do not endup in booting-time, and neither as
                                    seen as first time... would need to investigate why.
                                 */
                                //!seenFirstTime.contains(info.descriptiveId)
                        ) {
                            info = info.withNoDescriptiveId();
                        }
                        return info;
                    })
                    .forEach(e -> list.add(e));
        }

        if(fullyCovered){
            return list.stream().filter(e -> e.value == 1d).collect(Collectors.toList());
        }

        return list;
    }

    public static List<AdditionalInfo> getAdditionalInfoList(){
        return new ArrayList<>(ExecutionTracer.exposeAdditionalInfoList());
    }

    public static BootTimeObjectiveInfo getBootTimeObjectiveInfo(){
        return ObjectiveRecorder.exposeBootTimeObjectiveInfo();
    }

    public static void extractSpecifiedDto(List<String> dtoNames){
        UnitsInfoRecorder.registerSpecifiedDtoSchema(ExtractJvmClass.extractAsSchema(dtoNames));
    }

}
