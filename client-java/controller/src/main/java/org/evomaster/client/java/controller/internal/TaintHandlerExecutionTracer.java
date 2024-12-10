package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.sql.internal.TaintHandler;


/*
    FIXME: referring to ExecutionTracer is very tricky and not clean... works fine for embedded, but need
    custom hack for external to make it work.
    See ExternalSutController.getAdditionalInfoList()
    TODO possibly will need to refactor at some point...
*/
public class TaintHandlerExecutionTracer implements TaintHandler {
    @Override
    public void handleTaintForStringEquals(String left, String right, boolean ignoreCase) {

        ExecutionTracer.handleTaintForStringEquals(left, right, false);
    }

    @Override
    public void handleTaintForRegex(String value, String regex) {
        if(value == null || regex == null || !ExecutionTracer.isTaintInput(value)){
            return;
        }

        ExecutionTracer.addStringSpecialization(value,
                new StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, regex));
    }
}
