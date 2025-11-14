package org.evomaster.client.java.instrumentation;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.instrumentation.cfg.CFGRecorder;
import org.evomaster.client.java.instrumentation.cfg.ControlFlowGraph;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;

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

    /**
     * Expose the set of discovered Control Flow Graphs for instrumented methods.
     */
    public static List<ControlFlowGraph> getControlFlowGraphs(){
        return CFGRecorder.getAll();
    }

    /**
     * Compute and return ALL branch target numeric ids based on the complete CFG set.
     * This includes targets that have not been executed yet.
     */
    public static List<Integer> getAllBranchTargetIds(){
        List<Integer> ids = new ArrayList<>();
        for (ControlFlowGraph cfg : CFGRecorder.getAll()){
            String className = cfg.getClassName(); // bytecode name
            // Build per-line indices
            java.util.Set<Integer> allLines = new java.util.LinkedHashSet<>(cfg.getInstructionIndexToLineNumber().values());
            for (Integer line : allLines){
                java.util.List<Integer> branchIndices = cfg.getBranchInstructionIndicesForLine(line);
                for (int pos = 0; pos < branchIndices.size(); pos++){
                    int insnIdx = branchIndices.get(pos);
                    Integer opcode = cfg.getInstructionIndexToOpcode().get(insnIdx);
                    if (opcode == null) continue;
                    // create both true/false sides
                    String dTrue = org.evomaster.client.java.instrumentation.shared.ObjectiveNaming.branchObjectiveName(
                            className, line, pos, true, opcode);
                    String dFalse = org.evomaster.client.java.instrumentation.shared.ObjectiveNaming.branchObjectiveName(
                            className, line, pos, false, opcode);
                    int idT = ObjectiveRecorder.getMappedId(dTrue);
                    int idF = ObjectiveRecorder.getMappedId(dFalse);
                    ids.add(idT);
                    ids.add(idF);
                }
            }
        }
        return ids;
    }

    /**
     * Convenience: get coverage TargetInfo for all branch targets in the CFGs.
     */
    public static List<TargetInfo> getAllBranchTargetInfos(){
        List<Integer> ids = getAllBranchTargetIds();
        return getTargetInfos(ids, false, true);
    }

    /**
     * Parse a branch descriptive id created by ObjectiveNaming.branchObjectiveName into a structured descriptor.
     * Expected format:
     *  Branch_at_<class.with.dots>_at_line_00019_position_<pos>_(trueBranch|falseBranch)_<opcode>
     */
    public static BranchTargetDescriptor parseBranchDescriptiveId(String descriptiveId){
        if (descriptiveId == null || !descriptiveId.startsWith(ObjectiveNaming.BRANCH + "_at_")) {
            throw new IllegalArgumentException("Not a branch descriptive id: " + descriptiveId);
        }
        try {
            // strip "Branch_at_"
            String rest = descriptiveId.substring((ObjectiveNaming.BRANCH + "_at_").length());
            // split class and remainder
            int idxAtLine = rest.indexOf("_at_line_");
            String classDots = rest.substring(0, idxAtLine);
            String afterLine = rest.substring(idxAtLine + "_at_line_".length());
            // line is 5 digits padded; read until next underscore
            int idxPos = afterLine.indexOf("_position_");
            String linePadded = afterLine.substring(0, idxPos);
            int line = Integer.parseInt(linePadded);
            String afterPos = afterLine.substring(idxPos + "_position_".length());
            // afterPos: "<pos>_trueBranch_<opcode>" or "<pos>_falseBranch_<opcode>"
            int idxBranchTag = afterPos.indexOf("_" + ObjectiveNaming.TRUE_BRANCH.substring(1));
            boolean thenBranch;
            int posEndIdx;
            if (idxBranchTag >= 0) {
                thenBranch = true;
                posEndIdx = idxBranchTag;
            } else {
                String falseTag = "_" + ObjectiveNaming.FALSE_BRANCH.substring(1);
                idxBranchTag = afterPos.indexOf(falseTag);
                if (idxBranchTag < 0) {
                    throw new IllegalArgumentException("Missing branch tag in id: " + descriptiveId);
                }
                thenBranch = false;
                posEndIdx = idxBranchTag;
            }
            int position = Integer.parseInt(afterPos.substring(0, posEndIdx));
            // opcode after last underscore
            int lastUnderscore = afterPos.lastIndexOf('_');
            int opcode = Integer.parseInt(afterPos.substring(lastUnderscore + 1));
            return new BranchTargetDescriptor(classDots, line, position, thenBranch, opcode);
        } catch (RuntimeException ex){
            throw new IllegalArgumentException("Failed to parse branch descriptive id: " + descriptiveId, ex);
        }
    }

}
