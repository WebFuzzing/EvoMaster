package org.evomaster.clientJava.controller;

import org.evomaster.clientJava.controller.internal.SutController;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;



public abstract class EmbeddedSutController extends SutController {

    @Override
    public final boolean isInstrumentationActivated() {
        return InstrumentingAgent.isActive();
    }

    @Override
    public final void newSearch(){
        ExecutionTracer.reset();
        ObjectiveRecorder.reset();
    }

}
