package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.sql.internal.TaintHandler;

public class TaintHandlerExecutionTracer implements TaintHandler {
    @Override
    public void handleTaintForStringEquals(String left, String right, boolean ignoreCase) {
           /*
                FIXME: this is very tricky and not clean... works fine for embedded, but need
                custom hack for external to make it work. See ExternalSutContoller.getAdditionalInfoList()
             */
        ExecutionTracer.handleTaintForStringEquals(left, right, false);
    }
}
