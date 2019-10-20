package org.evomaster.client.java.controller;

import org.evomaster.client.java.controller.api.dto.ActionDto;
import org.evomaster.client.java.controller.api.dto.UnitsInfoDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.instrumentation.*;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;

import java.util.Collection;
import java.util.List;


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
}
