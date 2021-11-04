package org.evomaster.client.java.controller;

import org.evomaster.client.java.controller.api.dto.ActionDto;
import org.evomaster.client.java.controller.api.dto.UnitsInfoDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.instrumentation.*;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;

import java.util.Collection;
import java.util.List;

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
        /*
            We need to configure P6Spy for example, otherwise by default it will
            generate an annoying spy.log file
         */
        String driverName = getDatabaseDriverName();
        if(driverName != null) {
            InstrumentingAgent.initP6Spy(driverName);
        }
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
    public final List<AdditionalInfo> getAdditionalInfoList(){
        return InstrumentationController.getAdditionalInfoList();
    }

    @Override
    public final void newActionSpecificHandler(ActionDto dto){
        ExecutionTracer.setAction(new Action(dto.index, dto.inputVariables));
    }

    @Override
    public final UnitsInfoDto getUnitsInfoDto(){
         return getUnitsInfoDto(UnitsInfoRecorder.getInstance());
    }

    @Override
    public void setKillSwitch(boolean b) {
        ExecutionTracer.setKillSwitch(b);
    }
}
