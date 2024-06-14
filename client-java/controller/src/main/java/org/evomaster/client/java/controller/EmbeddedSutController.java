package org.evomaster.client.java.controller;

import org.evomaster.client.java.controller.api.dto.ActionDto;
import org.evomaster.client.java.controller.api.dto.BootTimeInfoDto;
import org.evomaster.client.java.controller.api.dto.UnitsInfoDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.instrumentation.*;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Main class used to implement an EvoMaster Driver.
 * A user that wants to use EvoMaster for white-box testing will
 * need to implement this abstract class.
 * </p>
 *
 * <p>
 * For full details on how to implement this class, look at the documentation
 * for <em>Write an EvoMaster Driver for White-Box Testing</em>,
 * currently at
 * <a href=https://github.com/EMResearch/EvoMaster/blob/master/docs/write_driver.md>
 *     https://github.com/EMResearch/EvoMaster/blob/master/docs/write_driver.md</a>
 * </p>
 */
public abstract class EmbeddedSutController extends SutController {

    @Override
    public final void setupForGeneratedTest(){
        //In the past, we configured P6Spy here
    }

    @Override
    public final boolean isInstrumentationActivated() {
        return InstrumentingAgent.isActive();
    }

    @Override
    public final void newSearch(){
        InstrumentationController.resetForNewSearch();
    }

    @Override
    public final void newTestSpecificHandler(){
        InstrumentationController.resetForNewTest();
    }

    @Override
    public final List<TargetInfo> getTargetInfos(Collection<Integer> ids){
        return InstrumentationController.getTargetInfos(ids);
    }

    @Override
    public final List<TargetInfo> getAllCoveredTargetInfos(){
        return InstrumentationController.getAllCoveredTargetInfos();
    }

    @Override
    public final List<AdditionalInfo> getAdditionalInfoList(){
        return InstrumentationController.getAdditionalInfoList();
    }

    @Override
    public final void newActionSpecificHandler(ActionDto dto){
        ExecutionTracer.setAction(new Action(
                dto.index,
                dto.name,
                dto.inputVariables,
                dto.externalServiceMapping.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new ExternalServiceMapping(e.getValue().remoteHostname, e.getValue().localIPAddress, e.getValue().signature, e.getValue().isActive))),
                dto.localAddressMapping,
                dto.skippedExternalServices.stream().map(e -> new ExternalService(e.hostname, e.port)).collect(Collectors.toList())
        ));
    }

    @Override
    public final UnitsInfoDto getUnitsInfoDto(){
         return getUnitsInfoDto(UnitsInfoRecorder.getInstance());
    }

    @Override
    public final void setKillSwitch(boolean b) {
        ExecutionTracer.setKillSwitch(b);
    }

    @Override
    public final void setExecutingInitSql(boolean executingInitSql) {
        ExecutionTracer.setExecutingInitSql(executingInitSql);
    }

    @Override
    public final void setExecutingInitMongo(boolean executingInitMongo) {
        ExecutionTracer.setExecutingInitMongo(executingInitMongo);
    }

    @Override
    public final void setExecutingAction(boolean executingAction){
        ExecutionTracer.setExecutingAction(executingAction);
    }

    @Override
    public BootTimeInfoDto getBootTimeInfoDto() {
        return getBootTimeInfoDto(InstrumentationController.getBootTimeObjectiveInfo());
    }

    @Override
    public final String getExecutableFullPath(){
        return null; //not needed for embedded
    }

    @Override
    public final void getJvmDtoSchema(List<String> dtoNames) {
        UnitsInfoRecorder.registerSpecifiedDtoSchema(ExtractJvmClass.extractAsSchema(dtoNames));
    }
}
