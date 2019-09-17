package org.evomaster.client.java.controller;

import org.evomaster.client.java.controller.api.dto.ActionDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.instrumentation.*;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Collection;
import java.util.List;


public abstract class EmbeddedSutController extends SutController {

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
}
