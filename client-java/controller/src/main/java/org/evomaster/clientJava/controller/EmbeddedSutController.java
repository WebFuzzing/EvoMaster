package org.evomaster.clientJava.controller;

import org.evomaster.clientJava.controller.internal.SutController;
import org.evomaster.clientJava.instrumentation.AdditionalInfo;
import org.evomaster.clientJava.instrumentation.InstrumentationController;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;
import org.evomaster.clientJava.instrumentation.TargetInfo;
import org.evomaster.clientJava.instrumentation.staticstate.ExecutionTracer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


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
    public final void newTest(){
        InstrumentationController.resetForNewTest();
        resetExtraHeuristics();
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
    public final void newAction(int actionIndex){
        ExecutionTracer.setActionIndex(actionIndex);
        resetExtraHeuristics();
    }
}
