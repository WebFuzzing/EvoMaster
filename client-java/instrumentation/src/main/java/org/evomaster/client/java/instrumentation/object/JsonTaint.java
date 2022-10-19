package org.evomaster.client.java.instrumentation.object;

import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Collection;

public class JsonTaint {

    /**
     * Check if given input is a valid taint for a JSON element.
     */
    public static void handlePossibleJsonTaint(String taint, Class<?> klass) {

        if (!ExecutionTracer.isTaintInput(taint)) {
            return;
        }

        StringSpecializationInfo info;

        if(Collection.class.isAssignableFrom(klass)){
            // TODO are there cases in which the content structure would be available? to check
            info = new StringSpecializationInfo(StringSpecialization.JSON_ARRAY,null);
        } else {
           info = new StringSpecializationInfo(StringSpecialization.JSON_OBJECT,
                   ClassToSchema.getOrDeriveNonNestedSchema(klass));
        }

        ExecutionTracer.addStringSpecialization(taint, info);
    }
}
